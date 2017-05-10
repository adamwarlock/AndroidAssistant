package com.example.vivek.assistant;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.AlarmClock;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.IntegerRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    //CHAT VARIABLES
    private EditText messageET;
    private ListView messagesContainer;
    private Button sendBtn;
    private chat_Adapter adapter;
    private ArrayList<chat_Message> chatHistory;

    //ASSISTANT S-TO-T && T-TO-S VARIABLES
    private TextToSpeech tts;
    private ArrayList<String> questions;
    private String name, surname, age, asName;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private static final String PREFS = "prefs";
    private static final String NEW = "new";
    private static final String NAME = "name";
    private static final String AGE = "age";
    private static final String AS_NAME = "as_name";

    //socket variables
    String serverAdd ="13.126.0.170";
    int port = 1234;
    Socket s;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initAssistant();
        initChat();
                               //con();                     #UNCOMMENT FOR SOCKET CONNECTION WITH SERVER

    }

    private void initAssistant(){
        preferences = getSharedPreferences(PREFS,0);
        editor = preferences.edit();

        findViewById(R.id.microphoneButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listen();
            }
        });

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "This Language is not supported");
                    }
                    speak("Hello");

                } else {
                    Log.e("TTS", "Initilization Failed!");
                }
            }
        });
        loadQuestions();

    }
    //LISTEN TO USER
    private void listen(){
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");

        try {
            startActivityForResult(i, 100);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(MainActivity.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
    }
    private void loadQuestions(){
        questions = new ArrayList<>();
        questions.clear();
        questions.add("Hello, what is your name?");
        questions.add("What is your surname?");
        questions.add("How old are you?");
        questions.add("That's all I had, thank you ");
        //client c=new client();
        //c.connect();

    }
    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
    private void speak(String text){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);

        }else{
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100){
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String inSpeech = res.get(0);
                recognition(inSpeech,true);
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_item_def, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();


        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void initChat() {
        messagesContainer = (ListView) findViewById(R.id.messagesContainer);
        messageET = (EditText) findViewById(R.id.messageEdit);
        sendBtn = (Button) findViewById(R.id.chatSendButton);

        TextView meLabel = (TextView) findViewById(R.id.meLbl);
        TextView companionLabel = (TextView) findViewById(R.id.friendLabel);
        RelativeLayout container = (RelativeLayout) findViewById(R.id.container);
        companionLabel.setText("Me");

        loadDummyHistory();

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageET.getText().toString();
                if (TextUtils.isEmpty(messageText)) {
                    return;
                }

                chat_Message chatMessage = new chat_Message();
                chatMessage.setId(122);//dummy
                chatMessage.setMessage(messageText);
                chatMessage.setDate(DateFormat.getDateTimeInstance().format(new Date()));
                chatMessage.setMe(true);

                messageET.setText("");

                displayMessage(chatMessage);
                recognition(messageText,false);
            }
        });


    }

    public void displayMessage(chat_Message message) {
        adapter.add(message);
        adapter.notifyDataSetChanged();
        scroll();
    }

    private void scroll() {
        messagesContainer.setSelection(messagesContainer.getCount() - 1);
    }

    private void loadDummyHistory(){

        chatHistory = new ArrayList<chat_Message>();

        chat_Message msg = new chat_Message();
        msg.setId(1);
        msg.setMe(false);
        if(preferences.getString(NAME,"").equals(""))
            msg.setMessage("Hello");
        else
            msg.setMessage("Hello "+ preferences.getString(NAME,""));

        msg.setDate(DateFormat.getDateTimeInstance().format(new Date()));
       // speak(msg.getMessage());
        chatHistory.add(msg);
        chat_Message msg1 = new chat_Message();
        msg1.setId(2);
        msg1.setMe(false);
        msg1.setMessage("How are you doing???");
        msg1.setDate(DateFormat.getDateTimeInstance().format(new Date()));
       // speak(msg1.getMessage());
        chatHistory.add(msg1);

        adapter = new chat_Adapter(MainActivity.this, new ArrayList<chat_Message>());
        messagesContainer.setAdapter(adapter);
       // String s=msg.getMessage();
       // s.concat(","+msg1.getMessage());
        for(int i=0; i<chatHistory.size(); i++) {
            chat_Message message = chatHistory.get(i);



            displayMessage(message);
        }
        //speak(s);

    }
    private void recognition(String text,boolean print){
        text=text.toLowerCase();
        if(print == true)
              createUserMsg(text);

        Log.e("Speech",""+text);
        String[] speech = text.split(" ");
        if(text.contains("hello")){

            createBotMsg(questions.get(0));

            speak(questions.get(0));
        }
        //
        if(text.contains("my name is")){
            name = speech[speech.length-1];
            Log.e("THIS", "" + name);
            editor.putString(NAME,name).apply();
            createBotMsg(questions.get(2));
            speak(questions.get(2));
        }
        //This must be the age
        if(text.contains("years") && text.contains("old")){
            String age = speech[speech.length-3];
            Log.e("THIS", "" + age);
            editor.putString(AGE, age).apply();
        }

        if(text.contains("what time is it") || text.contains("what is the time")){
            SimpleDateFormat sdfDate = new SimpleDateFormat("HH:mm");//dd/MM/yyyy
            Date now = new Date();
            String[] strDate = sdfDate.format(now).split(":");
            if(strDate[1].contains("00"))
                strDate[1] = "o'clock";
            createBotMsg("The time is " + sdfDate.format(now));
            speak("The time is " + sdfDate.format(now));

        }

        if(text.contains("wake me up at")){
            speak(speech[speech.length-1]);
            String[] time = speech[speech.length-1].split(":");
            String hour = time[0];
            String minutes = time[1];
           int h=Integer.valueOf(hour);
            int m= Integer.valueOf(minutes);
            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
            i.putExtra(AlarmClock.EXTRA_HOUR, h);
            i.putExtra(AlarmClock.EXTRA_MINUTES,m);
            startActivity(i);
            createBotMsg("Setting alarm to ring at " + h + ":" + m);
            speak("Setting alarm to ring at " + hour + ":" + minutes);
        }

        if(text.contains("thank you")){
            createBotMsg("Happy to  " + preferences.getString(NAME, null));
            speak("Happy to  " + preferences.getString(NAME, null));
        }

        if(text.contains("how old am i") || text.contains("what is my age") || text.contains("how old am I") ){
            String a=preferences.getString(AGE,"");
            if(a.equals(""))
            {
                createBotMsg("Sorry! I don't know how old are you.Please Tell me how old are you.");
                speak("Sorry! I don't know how old are you.Please Tell me how old are you.");
            }
            else {
                createBotMsg("You are " + preferences.getString(AGE, null) + " years old.");
                speak("You are " + preferences.getString(AGE, null) + " years old.");
            }
        }

        if(text.contains("what is your name")){
            String as_name = preferences.getString(AS_NAME,"");
            if(as_name.equals("")){
                createBotMsg("Oh! you have not given me any name. How do you want to call me?");
                speak("Oh! you have not given me any name. How do you want to call me?");
            }
            else{
                createBotMsg("My name is "+as_name);
                speak("My name is "+as_name);
            }
        }

        if(text.contains("call you")){
            String name = speech[speech.length-1];
            editor.putString(AS_NAME,name).apply();
            createBotMsg("I like it, thank you "+preferences.getString(NAME,null));
            speak("I like it, thank you "+preferences.getString(NAME,null));
        }

        if(text.contains("what is my name")){
            createBotMsg("Your name is "+preferences.getString(NAME,null));
            speak("Your name is "+preferences.getString(NAME,null));
        }

        if(text.contains("how are you")){
            String s=preferences.getString(NAME,"");
            if(s.equals("")){
                createBotMsg("I am fine. Thank You for asking.");
                speak("I am fine. Thank You for asking.");
            }
            else{
                createBotMsg("I am fine. Thank You for asking " + s);
                speak("I am fine. Thank You for asking " + s);
            }
        }
        if(text.contains("distance from") && text.contains("to")){
           createBotMsg("Loading maps application");
            speak("Loading maps application.");
            String dest=speech[speech.length-1];
            String src = speech[speech.length-3];
            Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                    Uri.parse("http://maps.google.com/maps?saddr="+src+"&daddr="+dest));
            startActivity(intent);
        }
        if(text.contains("route to")||text.contains("navigate to")){
            String dest=speech[speech.length-1];
            createBotMsg("Loading navigation");
            speak("Loading navigation.");
            Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q="+dest));
            startActivity(intent);
        }

    }
    private void createBotMsg(String text){
        chat_Message msg = new chat_Message();
        msg.setId(1);
        msg.setMe(false);
        msg.setMessage(text);
        msg.setDate(DateFormat.getDateTimeInstance().format(new Date()));
        displayMessage(msg);
    }
    private void createUserMsg(String text){
        chat_Message msg = new chat_Message();
        msg.setId(1);
        msg.setMe(true);
        msg.setMessage(text);
        msg.setDate(DateFormat.getDateTimeInstance().format(new Date()));
        displayMessage(msg);
    }

    /*
    void con(){
        new Thread(new clientThread()).start();
        try{
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            dout.writeUTF("Hello, Server");
            dout.flush();
            dout.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    private class clientThread implements Runnable {
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(serverAdd);
                s = new Socket(serverAddr, port);



                // s.close();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }
    */

}

