package com.example.vivek.assistant;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.IntegerRes;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

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
    JSONObject ans;
    //MESSAGING APP VARIABLES
    String numberID;
    String txtMessage;
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
    private void listen2(){
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Your Message");

        try {
            startActivityForResult(i, 200);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(MainActivity.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
    }
    private void listen3(){
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Your Message");

        try {
            startActivityForResult(i, 300);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(MainActivity.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
    }
    private void listen4(){
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Your Destination");

        try {
            startActivityForResult(i, 400);
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
              //  recognition(inSpeech);
                try {
                    initlizer(inSpeech);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if(requestCode == 200) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String inSpeech = res.get(0);
                //Toast.makeText(getApplicationContext(),inSpeech,Toast.LENGTH_LONG).show();
                txtMessage=inSpeech;
                smsMEssage(numberID,txtMessage);
            }
        }
        if(requestCode == 300) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String inSpeech = res.get(0);
                //Toast.makeText(getApplicationContext(),inSpeech,Toast.LENGTH_LONG).show();
                txtMessage=inSpeech;
                whatsappMessage(txtMessage);
            }
        }
        if(requestCode == 400) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String inSpeech = res.get(0);
                //Toast.makeText(getApplicationContext(),inSpeech,Toast.LENGTH_LONG).show();
                txtMessage=inSpeech;
                ubr(txtMessage);
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

               // displayMessage(chatMessage);
              //  recognition(messageText);
                try {
                    initlizer(messageText);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
    private void recognition(String text){
        text=text.toLowerCase();
       // if(print == true)
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
        if(text.contains("call")){
            String name=speech[speech.length-1];
            createBotMsg("Calling "+name);
            speak("Calling ");
            find(name);
        }
        if(text.contains("open ")){

            createBotMsg("opening "+speech[speech.length-1]);
            speak("opening "+speech[speech.length-1]);
            final PackageManager pm = getPackageManager();
            //get a list of installed apps.
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo packageInfo : packages) {


                if(packageInfo.packageName.contains(speech[speech.length-1])){
                    startNewActivity(this,packageInfo.packageName);
                    break;
                }
            }
        }
        if(text.contains("send sms to")||text.contains("send text to")){

            String pName=speech[speech.length-1];
            String cond = ContactsContract.Contacts.DISPLAY_NAME;
            Cursor cur = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                    null,
                    cond + " LIKE ?",
                    new String[]{pName}, null);

            if (cur.getCount() > 0) {
                cur.moveToNext();
                String name = cur
                        .getString(cur
                                .getColumnIndex(ContactsContract.Contacts._ID));


                Cursor pCur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " =?",
                        new String[]{name}, null);

                pCur.moveToNext();
                String number = pCur
                        .getString(pCur
                                .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                pCur.close();
                cur.close();

                if(number.contains("+91"))
                {
                    number=number.substring(1);
                }
                createBotMsg("What is the sms you want to send?");
                speak("What is the sms you want to send?");
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        listen2();
                    }
                }, 1500);

                numberID=number;


            }
            else{
                createBotMsg("No Contact found by that name.");
                speak("No Contact found by that name.");
            }
        }
        if(text.contains("send whatsapp message to")||text.contains("send whatsapp text to")||text.contains("whatsapp to ")){

            String pName=speech[speech.length-1];
            String cond = ContactsContract.Contacts.DISPLAY_NAME;
            Cursor cur = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                    null,
                    cond + " LIKE ?",
                    new String[]{pName}, null);

            if (cur.getCount() > 0) {
                cur.moveToNext();
                String name = cur
                        .getString(cur
                                .getColumnIndex(ContactsContract.Contacts._ID));


                Cursor pCur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " =?",
                        new String[]{name}, null);

                pCur.moveToNext();
                String number = pCur
                        .getString(pCur
                                .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                pCur.close();
                cur.close();

                if(number.contains("+91"))
                {
                    number=number.substring(1);
                }
                numberID=number;
                createBotMsg("opening whatsapp");
                speak("opening whatsapp");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Uri mUri = Uri.parse("smsto:"+numberID);
                        Intent mIntent = new Intent(Intent.ACTION_SENDTO,mUri);
                        mIntent.setPackage("com.whatsapp");
                        //mIntent.setType("text/plain");
                      //  mIntent.putExtra(Intent.EXTRA_TEXT, "The text goes here");
                       // mIntent.putExtra("chat",true);
                        startActivity(mIntent);

                    }
                }, 600);





            }
            else{
                createBotMsg("No Contact found by that name.");
                speak("No Contact found by that name.");
            }
        }
        if(text.contains("send message by whatsapp")){

                createBotMsg("what is the message?");
                speak("what is the message?");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        listen3();
                    }
                }, 1000);






        }
        if(text.contains("book me a ride")||text.contains("book me an uber")){
            createBotMsg("what is your destination?");
            speak("what is your destination?");
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    listen4();
                }
            }, 1500);
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


    public void find(String pName) {

        String cond = ContactsContract.Contacts.DISPLAY_NAME;
        Cursor cur = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                null,
                cond + " LIKE ?",
                new String[]{pName}, null);
        //Toast.makeText(getApplicationContext(), cond, Toast.LENGTH_LONG).show();
        if (cur.getCount() > 0) {
            cur.moveToNext();
            String name = cur
                    .getString(cur
                            .getColumnIndex(ContactsContract.Contacts._ID));

            //t2.append(name);
            Cursor pCur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " =?",
                    new String[]{name}, null);

            pCur.moveToNext();
            String number = pCur
                    .getString(pCur
                            .getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            //t2.append("," + number);
            pCur.close();
            cur.close();
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:0" + number));
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            createBotMsg("Calling "+pName);
            speak("Calling "+pName);
            startActivity(callIntent);
        }
        else{
            createBotMsg("No Contact found by that name.");
            speak("No Contact found by that name.");
            //Toast.makeText(getApplicationContext(),"NO CONTACTS",Toast.LENGTH_LONG).show();
        }


    }
    public void startNewActivity(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            // We found the activity now start the activity
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } else {
            // Bring user to the market or let them choose an app?
            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id=" + packageName));
            context.startActivity(intent);
        }
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void initlizer(String text) throws JSONException {
        if(isNetworkAvailable()){
            //volley
         //   Toast.makeText(getApplicationContext(),text,Toast.LENGTH_LONG).show();
            func(text);
            createUserMsg(text);
            //Toast.makeText(getApplicationContext(),ans.getString("command"),Toast.LENGTH_LONG).show();}
            //funcCalls(ans);
        }
        else{
            recognition(text);
        }
    }

    private void funcCalls(JSONObject ans) throws JSONException {

        String command=ans.getString("command").toLowerCase();
        //Toast.makeText(getApplicationContext(),command,Toast.LENGTH_LONG).show();
        if(command.contains("call")){
            String param=ans.getString("param1");

           // Toast.makeText(getApplicationContext(),param,Toast.LENGTH_LONG).show();
            find(param);
        }
        else if(command.contains("not found")){
            String param=ans.getString("param1");
            recognition(param);
        }


    }



    public boolean func(final String text){

        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://13.126.31.42:80/data";

        StringRequest req=new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                try {
                    JSONObject j=new JSONObject(response);
                        ans=j;

                    funcCalls(ans);
                   // Toast.makeText(getApplicationContext(),"after  funccalls",Toast.LENGTH_LONG).show();
                    //return j;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }){
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                final String s=text;
                params.put("key", s);

                return params;
            }
        };
        queue.add(req);
        Toast.makeText(getApplicationContext(),"send",Toast.LENGTH_LONG).show();
        return true;
    }
    private void smsMEssage(String number,String message){
        Uri uri = Uri.parse("smsto:" + number);
        Intent sendIntent = new Intent(Intent.ACTION_SENDTO, uri);



        sendIntent.putExtra("sms_body", message);

        startActivity(sendIntent);
    }
    private void whatsappMessage(String message){
        Intent ntent = new Intent(Intent.ACTION_SEND);
        ntent.setPackage("com.whatsapp");
        ntent.setType("text/plain");
        ntent.putExtra(Intent.EXTRA_TEXT, message);
        ntent.putExtra("chat",true);
        startActivity(ntent);
    }
    private void ubr(String add){
        Geocoder coder = new Geocoder(getApplicationContext(), Locale.getDefault());
        List<Address> address;
        // GeoPoint p1 = null;

        try {
            address = coder.getFromLocationName(add,1);
            if (address==null) {
               createBotMsg("Address not found");
                speak("Address not found");
            }else {
                Address location = address.get(0);

                double lat = location.getLatitude();
                double lng = location.getLongitude();

                String uri =
                        "uber://?action=setPickup&pickup=my_location&dropoff[latitude]="+String.valueOf(lat)+"&dropoff[longitude]="+String.valueOf(lng);
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse(uri));
                startActivity(intent);
            }
        } catch (IOException e) {

        }
    }
}

