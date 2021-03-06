package com.example.CSWordApp;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GestureDetectorCompat;

import com.example.CSWordApp.Adapters.WordsListAdapter;
import com.example.CSWordApp.Data.Word;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("FieldCanBeLocal")
public class WordsActivity extends AppCompatActivity {
    private final static String TAG = "WordActivity";
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private FirebaseDatabase database;
    private DatabaseReference userReference;

    private GestureDetectorCompat swipeListener;

    private Button saveWord;
    private TextInputLayout word;
    private TextInputLayout translation;
    private TextInputLayout usageExample;

    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_words);

        toolbar = findViewById(R.id.toolbar);
        //toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_32);
        toolbar.setTitle("Мої слова");
        setSupportActionBar(toolbar);

        Drawable backArrow = ResourcesCompat.getDrawable(this.getResources(), R.drawable.ic_baseline_arrow_back_32, null);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(backArrow);

        swipeListener = new GestureDetectorCompat(this, new SwipeGestureListener(this));

        initFirebase();


        userReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateWordsList(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }

        });

        saveWord = findViewById(R.id.save_word);
        saveWord.setOnClickListener(v -> saveWordToDb());
    }

    private void initFirebase(){
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance(getResources().getString(R.string.realtime_db_reference));
        userReference = database.getReference("users").child(currentUser.getUid()).child("words");
        userReference.keepSynced(true);
    }

    private void updateWordsList(DataSnapshot snapshot) {
        GenericTypeIndicator<HashMap<String, Word>> type = new GenericTypeIndicator<HashMap<String, Word>>() {};
        HashMap<String, Word> hashMap = snapshot.getValue(type);
        if( hashMap == null){
            return;
        } else{
            ArrayList<Word> wordEntities = new ArrayList<>(hashMap.values());

            ListView wordsList = findViewById(R.id.words_list);

            WordsListAdapter wordsListAdapter = new WordsListAdapter(this, R.layout.word_item_layout, wordEntities, userReference);
            wordsList.setAdapter(wordsListAdapter);

        }
    }

    private void saveWordToDb() {
        word = findViewById(R.id.word);
        translation = findViewById(R.id.translation);
        usageExample = findViewById(R.id.usage_example);

        String wordText = word.getEditText().getText().toString();
        word.getEditText().getText().clear();
        String translationText = translation.getEditText().getText().toString();
        translation.getEditText().getText().clear();
        String usageExampleText = usageExample.getEditText().getText().toString();
        usageExample.getEditText().getText().clear();

        if(wordText.isEmpty() || translationText.isEmpty()){
            Toast.makeText(this, R.string.necessary_word_alert, Toast.LENGTH_SHORT).show();
            return;
        }

        Word wordEntity = new Word(wordText, translationText, usageExampleText);

        Query wordData = userReference.child(wordEntity.getWord());
        try {
            wordData.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Word thisWord = snapshot.getValue(Word.class);

                    if( thisWord == null){
                        userReference.child(wordEntity.getWord()).setValue(wordEntity);
                        return;
                    }

                    if(!thisWord.getTranslations().contains(wordEntity.getTranslations())){
                        String updatedTranslation = thisWord.getTranslations() +"\n" +wordEntity.getTranslations();
                        userReference.child(wordEntity.getWord()).child("translations").setValue(updatedTranslation);
                    }

                    if(!thisWord.getUsageExamples().contains(wordEntity.getUsageExamples()) || thisWord.getUsageExamples().isEmpty()){
                        String updatedUsageExamples = wordEntity.getUsageExamples() +"\n" + thisWord.getUsageExamples();
                        userReference.child(wordEntity.getWord()).child("usageExamples").setValue(updatedUsageExamples);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.w(TAG, error.getMessage());
                }
            });
        } catch (Exception e){
            Log.d(TAG, e.getMessage(), e);
            return;
        }
        Toast.makeText(this, R.string.save_word_success, Toast.LENGTH_SHORT).show();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return swipeListener.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {    //закрити віртуальну клаву, якщо клік за межі поля вводу
        View view = getCurrentFocus();
        boolean ret = super.dispatchTouchEvent(event);

        if (view instanceof EditText) {
            View w = getCurrentFocus();
            int[] scrCoordinates = new int[2];
            w.getLocationOnScreen(scrCoordinates);
            float x = event.getRawX() + w.getLeft() - scrCoordinates[0];
            float y = event.getRawY() + w.getTop() - scrCoordinates[1];

            if (event.getAction() == MotionEvent.ACTION_UP
                    && (x < w.getLeft() || x >= w.getRight()
                    || y < w.getTop() || y > w.getBottom()) ) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getWindow().getCurrentFocus().getWindowToken(), 0);
            }
        }
        return ret;

    }

    
}