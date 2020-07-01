package com.gpayinterns.chat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.android.volley.ClientError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import static com.gpayinterns.chat.ServerConstants.BASE_URL;
import static com.gpayinterns.chat.ServerConstants.CHATS;
import static com.gpayinterns.chat.ServerConstants.END_MESSAGE;
import static com.gpayinterns.chat.ServerConstants.MESSAGES;
import static com.gpayinterns.chat.ServerConstants.START_MESSAGE;
import static com.gpayinterns.chat.ServerConstants.USERS;

/**
 * ViewMessageActivity gets launched when the user clicks on a contact to view the messages.
 * It performs it's operation in the following manner:
 * 1. Call "firstReceiveMessageFromServer()" method to receive messages previous to lastMessageID
 * 2. Call "receiveMessageFromServer()" method periodically to receive messages after lastMessageID
 * 3. Call "receivePreviousMessagesFromServer()" method to receive messages after user has hit top of the screen.
 */
public class ViewMessageActivity extends AppCompatActivity
{

    public static final String CHAT_ID = "CHAT_ID";
    public static final String CONTACT_USERNAME = "CONTACT_USERNAME";
    public static final String LAST_MESSAGE_ID = "LAST_MESSAGE_ID";
    private static final String POLL = "SHORT_POLLING";
    private static final int SELECT_PICTURE = 0;

    private static boolean active=false;

    private List<Message> messages = new ArrayList<Message>();
    Set<String> messageIDSet = new HashSet<String>();//This helps to prevent duplicate messages.

    EditText messageEditText;
    MessageRecyclerAdapter messageRecyclerAdapter;
    private RecyclerView recyclerMessages;
    LinearLayoutManager messageLayoutManager;
    SwipeRefreshLayout swipeRefreshLayout;

    private String currentUser;
    private String chatID;
    private String lastMessageID;

    private Timer mTimer;
    private ProgressBar progressBar;

    @Override
    protected void onPause()
    {
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        if(mTimer != null)
            mTimer.cancel();
        VolleyController.getInstance(this).getRequestQueue().cancelAll(POLL);
        active=false;
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        setContentView(R.layout.activity_view_message);

        getCurrentUser();
        messageEditText=(EditText)findViewById(R.id.send_message_text);
        progressBar = (ProgressBar) findViewById(R.id.view_message_indeterminateBar);

        Objects.requireNonNull(getSupportActionBar()).setTitle(getIntent().getStringExtra(CONTACT_USERNAME));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initializeDisplayContent();
        final Button button = findViewById(R.id.send_message_button);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String messageText = messageEditText.getText().toString().trim();
                ImageView sendImage = (ImageView) findViewById(R.id.send_image);
                if( sendImage.getDrawable()!=null)
                {
                    //TODO send image to server
                    sendImageToServer();
                    addImageToScreen();
//                    removeImageFromEditText();
                    hideSoftKeyboard();
                    return;
                }
                if(messageText.equals(""))
                {
                    hideSoftKeyboard();
                    return;
                }
                try
                {
                    sendMessageToServer(messageText);
                    messageEditText.setText("");
                    hideSoftKeyboard();
                }
                catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private void sendImageToServer()
    {
        ImageView sendImage = (ImageView) findViewById(R.id.send_image);
        sendImage.invalidate();
        BitmapDrawable drawable = (BitmapDrawable) sendImage.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        uploadBitmap(bitmap);


//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
//        byte[] byteArray = byteArrayOutputStream .toByteArray();
//
//        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);

    }
    private void uploadBitmap(final Bitmap bitmap)
    {
        String URL = BASE_URL + USERS
                + "/" + currentUser + "/" + CHATS
                + "/" + chatID + "/" + MESSAGES;
        VolleyMultipartRequest volleyMultipartRequest = new VolleyMultipartRequest(Request.Method.POST, URL,
                new Response.Listener<NetworkResponse>()
                {
                    @Override
                    public void onResponse(NetworkResponse response)
                    {
                        String resultResponse = new String(response.data);
                        try
                        {
                            JSONObject result = new JSONObject(resultResponse);
                            String message = result.getString("message");
                            Log.d("messageReceived",message);
                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        NetworkResponse networkResponse = error.networkResponse;
                        String errorMessage = "Unknown error";
                        if (networkResponse == null)
                        {
                            if (error.getClass().equals(TimeoutError.class))
                            {
                                errorMessage = "Request timeout";
                            }
                            else if (error.getClass().equals(NoConnectionError.class))
                            {
                                errorMessage = "Failed to connect server";
                            }
                        }
                        Log.d("Error", errorMessage);
                        error.printStackTrace();
                    }
                }) {
            @Override
            protected Map<String, DataPart> getByteData()
            {
                Map<String, DataPart> params = new HashMap<>();
                long imageName = System.currentTimeMillis();
                ImageView sendImage = (ImageView) findViewById(R.id.send_image);
                params.put("file", new DataPart(imageName + ".png", AppHelper.getFileDataFromDrawable(getBaseContext(),sendImage.getDrawable()),"image/jpeg"));
                return params;
            }
        };

        Volley.newRequestQueue(this).add(volleyMultipartRequest);
    }
    private void removeImageFromEditText()
    {
        ImageView sendImage = (ImageView) findViewById(R.id.send_image);
        sendImage.setImageDrawable(null);
        messageEditText.setVisibility(View.VISIBLE);
    }

    private void addImageToScreen()
    {
        List <Message> newMessage = new ArrayList<Message>();
        ImageView sendImage = (ImageView) findViewById(R.id.send_image);
        BitmapDrawable drawable = (BitmapDrawable) sendImage.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        newMessage.add(new Message("0",chatID,false,"", Long.toString(System.currentTimeMillis()),bitmap));
        messageRecyclerAdapter.addMessages(newMessage);
        recyclerMessages.smoothScrollToPosition(recyclerMessages.getAdapter().getItemCount()-1);
    }

    private void sendMessageToServer(String messageText) throws JSONException
    {
        String URL = BASE_URL + USERS
                + "/" + currentUser + "/" + CHATS
                + "/" + chatID + "/" + MESSAGES;

        JSONObject jsonBody = new JSONObject();
        Log.d("messageText sent to server: ",messageText);
        jsonBody.put("contentType", "text");
        jsonBody.put("textContent",messageText);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, URL, jsonBody, new Response.Listener<JSONObject>()
                {


                    @Override
                    public void onResponse(JSONObject response)
                    {
                        Log.d("ResponseMessage: " , response.toString());
                        try
                        {
                            String message = response.getString("message");
                            if(message.equals("Success"))
                            {
                                String lastMessageID = response.getString("MessageId");
                            }
                        }
                        catch (JSONException e)
                        {
                            Toast.makeText(getApplicationContext(), "Parse Error", Toast.LENGTH_SHORT).show();
                            Log.d("JsonError: ",e.toString());
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        Log.d("errorMessage",error.toString());
                        if (error instanceof TimeoutError || error instanceof NoConnectionError)
                        {
                            Toast.makeText(getApplicationContext(), "Network timeout", Toast.LENGTH_LONG).show();
                        }
                        else if(error instanceof ClientError)
                        {
                            String responseBody = null;
                            responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            JSONObject data = null;
                            try
                            {
                                data = new JSONObject(responseBody);
                            }
                            catch (JSONException e)
                            {
                                e.printStackTrace();
                            }

                            assert data != null;
                            String message = data.optString("Message");
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
        {
            @Override
            public Priority getPriority()
            {
                return Priority.IMMEDIATE;
            }
        };
        VolleyController.getInstance(this).addToRequestQueueWithRetry(jsonObjectRequest);
    }

    private void getCurrentUser()
    {
        SharedPreferences mPrefs= getSharedPreferences("CHAT_LOGGED_IN_USER", 0);
        currentUser = mPrefs.getString("currentUser","");
    }

    public class LinearLayoutManagerWrapper extends LinearLayoutManager
    {
        public LinearLayoutManagerWrapper(Context context)
        {
            super(context);
        }

        @Override
        public boolean supportsPredictiveItemAnimations()
        {
            return false;
        }
    }

    private void initializeDisplayContent()
    {
        recyclerMessages = (RecyclerView) findViewById(R.id.message_recyclerView);
        messageLayoutManager = new LinearLayoutManagerWrapper(this);
        messageRecyclerAdapter = new MessageRecyclerAdapter(this,messages);
        messageLayoutManager.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(messageLayoutManager);
        recyclerMessages.setAdapter(messageRecyclerAdapter);
        recyclerMessages.addOnScrollListener(new RecyclerView.OnScrollListener()
        {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState)
            {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(-1))//-1 implies up
                {
                    //User has hit top of view
                    receivePreviousMessagesFromServer();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == SELECT_PICTURE && data!=null)
        {
            Uri selectedImage = data.getData();
            ImageView sendImage = (ImageView) findViewById(R.id.send_image);
            sendImage.setImageURI(selectedImage);
            messageEditText.setVisibility(View.INVISIBLE);
        }
    }

    private void receivePreviousMessagesFromServer()
    {
        if(messages.isEmpty())
        {
            return;
        }
        String firstMessageID = messages.get(0).messageID;
        String URL = BASE_URL + USERS +
                "/" + currentUser + "/" + CHATS +
                "/" + chatID + "/" + MESSAGES +
                "?" + END_MESSAGE + "=" + firstMessageID;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, URL, null, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        Log.d("ResponseMessage: " , response.toString());
                        if(swipeRefreshLayout.isRefreshing())
                        {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                        try
                        {
                            JSONArray messageList = response.getJSONArray("payload");
                            List <Message> newMessages = new ArrayList<Message>();
                            for(int i=0;i<messageList.length();i++)
                            {
                                JSONObject message = (JSONObject) messageList.get(i);
                                Message newMessage = jsonToMessage(message);
                                if(!messageIDSet.contains(newMessage.messageID))
                                {
                                    newMessages.add(newMessage);
                                    messageIDSet.add(newMessage.messageID);
                                }
                            }
                            if(newMessages.size()>0)
                            {
                                messageRecyclerAdapter.addMessagesToFront(newMessages);
                            }
                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        // TODO: Handle error

                    }
                }){
            @Override
            public Priority getPriority()
            {
                return Priority.IMMEDIATE;
            }
        };

        VolleyController.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        active=true;

        findViewById(R.id.view_message_constraint_layout).requestFocus();
        chatID = getIntent().getStringExtra(CHAT_ID);
        lastMessageID = getIntent().getStringExtra(LAST_MESSAGE_ID);

        messageIDSet.clear();
        messages.clear();

        hideSoftKeyboard();
        firstReceiveMessageFromServer();
    }

    private void ReceiveMessagePeriodically()
    {
        final Handler handler = new Handler();
        mTimer = new Timer();
        TimerTask task = new TimerTask()
        {
            @Override
            public void run()
            {
                handler.post(new Runnable()
                {
                    public void run()
                    {
                        receiveMessageFromServer();
                    }
                });
            }
        };
        mTimer.schedule(task, 0, 5000);//runs it every 5 seconds;
    }

    private void firstReceiveMessageFromServer()
    {
        String URL = BASE_URL + USERS +
                "/" + currentUser + "/" +
                CHATS + "/" + chatID + "/" +
                MESSAGES + "?" + END_MESSAGE + "=" + lastMessageID;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, URL, null, new Response.Listener<JSONObject>()
                {

                    @Override
                    public void onResponse(JSONObject response)
                    {
                        Log.d("ResponseMessage: " , response.toString());
                        try
                        {
                            JSONArray messageList = response.getJSONArray("payload");
                            List <Message> newMessages = new ArrayList<Message>();
                            for(int i=0;i<messageList.length();i++)
                            {
                                JSONObject message = (JSONObject) messageList.get(i);
                                Message newMessage = jsonToMessage(message);

                                if(!messageIDSet.contains(newMessage.messageID))
                                {
                                    newMessages.add(newMessage);
                                    messageIDSet.add(newMessage.messageID);
                                }
                            }
                            if(newMessages.size()>0 && active)
                            {
                                lastMessageID = newMessages.get(newMessages.size()-1).messageID;
                                messageRecyclerAdapter.addMessages(newMessages);
                                recyclerMessages.smoothScrollToPosition(recyclerMessages.getAdapter().getItemCount()-1);
                                progressBar.setVisibility(View.GONE);
                            }
                            ReceiveMessagePeriodically();
                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        // TODO: Handle error

                    }
                }){
            @Override
            public Priority getPriority()
            {
                return Priority.IMMEDIATE;
            }
        };

        VolleyController.getInstance(this).addToRequestQueueWithRetry(jsonObjectRequest);
    }

    private void receiveMessageFromServer()
    {
        Log.d("currentUser",currentUser);
        Log.d("chatID:",chatID);
        Log.d("lastMessageID:",lastMessageID);
        String URL = BASE_URL + USERS +
                "/" + currentUser + "/" + CHATS +
                "/" + chatID + "/" + MESSAGES + "?" +
                START_MESSAGE + "=" + lastMessageID;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, URL, null, new Response.Listener<JSONObject>()
                {

                    @Override
                    public void onResponse(JSONObject response)
                    {
                        Log.d("ResponseMessage: " , response.toString());
                        try
                        {
                            JSONArray messageList = response.getJSONArray("payload");
                            List <Message> newMessages = new ArrayList<Message>();
                            for(int i=0;i<messageList.length();i++)
                            {
                                JSONObject message = (JSONObject) messageList.get(i);
                                Message newMessage = jsonToMessage(message);
                                if(!messageIDSet.contains(newMessage.messageID))
                                {
                                    newMessages.add(newMessage);
                                    messageIDSet.add(newMessage.messageID);
                                }
                                if(i==messageList.length()-1)
                                {
                                    lastMessageID = newMessage.messageID;
                                }
                            }
                            if(newMessages.size()>0 && active)
                            {
                                lastMessageID = newMessages.get(newMessages.size()-1).messageID;
                                messageRecyclerAdapter.addMessages(newMessages);
                                recyclerMessages.smoothScrollToPosition(recyclerMessages.getAdapter().getItemCount()-1);
                            }
                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        // TODO: Handle error

                    }
                }){
            @Override
            public Priority getPriority()
            {
                return Priority.LOW;
            }
        };
        jsonObjectRequest.setTag(POLL);
        VolleyController.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }

    private void addMessageToScreen(String messageID)
    {
        messageIDSet.add(messageID);
        lastMessageID = messageID;
        findViewById(R.id.view_message_constraint_layout).requestFocus();
        List <Message> newMessage = new ArrayList<Message>();
        newMessage.add(new Message(messageID,chatID,false,messageEditText.getText().toString(),"0",null));
        Log.d("here",Integer.toString(newMessage.size()));
        messageRecyclerAdapter.addMessages(newMessage);
        recyclerMessages.smoothScrollToPosition(recyclerMessages.getAdapter().getItemCount()-1);
    }

    private void pickImage()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PICTURE);
    }

    private void hideSoftKeyboard()
    {
        View view = this.getCurrentFocus();
        if (view != null)
        {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            assert imm != null;
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private Message jsonToMessage(JSONObject message) throws JSONException
    {
        String messageID = message.getString("MessageId");
        String chatID = message.getString("ChatId");
        boolean received = !message.getBoolean("SentByCurrentUser");
        String text = message.getString("TextContent");
        JSONObject sendTime = message.getJSONObject("CreationTs");
        String seconds = sendTime.getString("seconds");

        return new Message(messageID,chatID,received,text,seconds+"000",null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.view_messages_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle item selection
        switch (item.getItemId())
        {
            case R.id.menu_send_image:
                pickImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}