package com.gpayinterns.chat;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private static final int SELECT_FILE = 0;

    private static boolean active = false;

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
    private String username;

    private Timer mTimer;// helps to send requests for newer messages periodically
    private ProgressBar progressBar;

    private Uri fileUri;//stores the uri of the file received upon selection from file explorer

    @Override
    protected void onPause()
    {
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        if(mTimer != null)
        {
            //stop sending requests to receive new messages
            mTimer.cancel();
        }
        VolleyController.getInstance(this).getRequestQueue().cancelAll(POLL);//cancel all previous volley requests
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
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.view_message_swipe_refresh);

        username = getIntent().getStringExtra(CONTACT_USERNAME);
        Objects.requireNonNull(getSupportActionBar()).setTitle(username);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        fileUri = null;

        initializeDisplayContent();
        chatID = getIntent().getStringExtra(CHAT_ID);
        lastMessageID = getIntent().getStringExtra(LAST_MESSAGE_ID);
        if(chatID == null)//i.e this activity was launched using search by phoneNum
        {
            // Since there isn't any chatID and lastMessageID received from the previous activity,
            // these details have to be fetched from the server
            // 1. get chatID using createChat
            // 2. get lastMessageID using getChat
            try
            {
                loadChatIDFromServer();
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }

        final Button button = findViewById(R.id.send_message_button);
        button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String messageText = messageEditText.getText().toString().trim();
                if(messageText.equals(""))
                {
                    hideSoftKeyboard();
                    return;
                }
                sendMessageToServer(messageText);
                messageEditText.setText("");
                hideSoftKeyboard();
            }
        });
    }

    /**
     * This method sends a richText which is referenced by fileUri
     */
    private void sendFileToServer()
    {
        String URL = BASE_URL + USERS
                + "/" + currentUser + "/" + CHATS
                + "/" + chatID + "/" + MESSAGES;
        Log.d("chatID",chatID);
        Log.d("UserId",currentUser);
        VolleyMultipartRequest volleyMultipartRequest = new VolleyMultipartRequest(Request.Method.POST, URL,
                new Response.Listener<NetworkResponse>()
                {
                    @Override
                    public void onResponse(NetworkResponse response)
                    {
                        String resultResponse = new String(response.data);
                        Log.d("ServerResponse",resultResponse);
                        try
                        {
                            JSONObject result = new JSONObject(resultResponse);
                            String message = result.getString("message");
                            Log.d("messageReceived",message);
                            String messageID = result.getString("MessageId");
                            String mimeType = getMimeType(fileUri);
                            String fileName = getFileName(fileUri);

                            AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(fileUri,"r");
                            assert afd != null;
                            long size = afd.getLength();
                            afd.close();

                            addFileToScreen(messageID,fileName,mimeType,size+" B");
                            String outputPath = ViewMessageActivity.this.getFilesDir().getAbsolutePath() + "/" + fileName;
//                            copyFileToLocalCache(outputPath);
//                            new UpdateCache().execute(messageID,outputPath);
                        }
                        catch (JSONException | IOException e)
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
                        handleError(error);
                    }
                }) {
            @Override
            protected Map<String, DataPart> getByteData() throws IOException
            {
                Map<String, DataPart> params = new HashMap<>();
                String fileName = getFileName(fileUri);
                String mimeType = getMimeType(fileUri);
                byte [] array = AppHelper.getBytes(ViewMessageActivity.this,fileUri);

                params.put("file", new DataPart(fileName , array,mimeType));
                return params;
            }

            @Override
            public Priority getPriority()
            {
                return Priority.IMMEDIATE;
            }
        };

        VolleyController.getInstance(this).addToRequestQueue(volleyMultipartRequest);
    }

    /**
     * The file referenced by fileUri will be stored at the path provided as the argument
     * @param outputPath path where the file will be stored
     */
    private void copyFileToLocalCache(String outputPath)
    {
        File source = new File(fileUri.getPath());

        ContentResolver resolver = getApplicationContext()
                .getContentResolver();
        try (InputStream in = resolver.openInputStream(fileUri))
        {
            File dir = new File (outputPath);
            //create a new directory if outputPath doesn't exist
            if (!dir.exists())
            {
                dir.mkdirs();
            }
            OutputStream out = new FileOutputStream(outputPath);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1)
            {
                out.write(buffer, 0, read);
            }
            in.close();
            // write the output file
            out.flush();
            out.close();
            Log.d("database","files copied to desired location");
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void addFileToScreen(String messageID,String fileName,String mimeType,String fileSize)
    {
        if(messageIDSet.contains(messageID))
        {
            return ;
        }
        messageIDSet.add(messageID);
        lastMessageID = messageID;
        findViewById(R.id.view_message_constraint_layout).requestFocus();
        List <Message> newMessage = new ArrayList<Message>();
        newMessage.add(new Message(messageID,chatID,false,null,Long.toString(System.currentTimeMillis()),fileName,mimeType,fileSize));
        messageRecyclerAdapter.addMessages(newMessage);
        recyclerMessages.smoothScrollToPosition(recyclerMessages.getAdapter().getItemCount()-1);
    }

    /**
     * This method helps to send a text message to the server
     * @param messageText the text message to be sent
     */
    private void sendMessageToServer(final String messageText)
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
                        Log.d("ServerResponse",resultResponse);
                        JSONObject result = null;
                        try
                        {
                            result = new JSONObject(resultResponse);
                            String messageId = result.getString("MessageId");
                            String message = result.getString("message");
                            if(message.equals("Success"))
                            {
                                addMessageToScreen(messageId,messageText);
                            }
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
                        handleError(error);
                    }
                }) {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String> params = new HashMap<>();
                params.put("textContent", messageText);
                return params;
            }

            @Override
            public Priority getPriority()
            {
                return Priority.IMMEDIATE;
            }
        };

        VolleyController.getInstance(this).addToRequestQueue(volleyMultipartRequest);
    }

    private void getCurrentUser()
    {
        SharedPreferences mPrefs= getSharedPreferences("CHAT_LOGGED_IN_USER", 0);
        currentUser = mPrefs.getString("currentUser","");
    }

    private void initializeDisplayContent()
    {
        recyclerMessages = (RecyclerView) findViewById(R.id.message_recyclerView);
        messageLayoutManager = new LinearLayoutManager(this);
        messageRecyclerAdapter = new MessageRecyclerAdapter(this,messages,currentUser);
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
        if(resultCode == RESULT_OK && requestCode == SELECT_FILE && data!=null)
        {
            fileUri = data.getData();
            try
            {
                //show the dialog to send the file
                showFileDialog();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method gets called when the user hits the top of the view (to facilitate pagination)
     */
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
                        if(swipeRefreshLayout!=null && swipeRefreshLayout.isRefreshing())
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
                        handleError(error);
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
        mTimer.schedule(task, 0, 5000);//request for new messages every 5 seconds;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Runtime.getRuntime().gc();// free heap memory by destroying unreachable objects
    }

    /**
     * receive messages for the first time when the activity launches
     * i.e messages before lastMessageID
     */
    private void firstReceiveMessageFromServer()
    {
        String URL = BASE_URL + USERS +
                "/" + currentUser + "/" +
                CHATS + "/" + chatID + "/" +
                MESSAGES + "?" + END_MESSAGE + "=" + lastMessageID;

        if(lastMessageID==null)
        {
           URL = BASE_URL + USERS +
                   "/" + currentUser + "/" +
                   CHATS + "/" + chatID + "/" + MESSAGES;
        }
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
                            }
                            progressBar.setVisibility(View.GONE);
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
                        handleError(error);
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

    /**
     * 1. fetch messages after lastMessageID
     * 2. update lastMessageID if newer messages found
     */
    private void receiveMessageFromServer()
    {
        String URL = BASE_URL + USERS +
                "/" + currentUser + "/" + CHATS +
                "/" + chatID + "/" + MESSAGES + "?" +
                START_MESSAGE + "=" + lastMessageID;
        if(lastMessageID == null)
        {
            URL = BASE_URL + USERS +
                    "/" + currentUser + "/" + CHATS +
                    "/" + chatID + "/" + MESSAGES;
        }

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
                        handleError(error);
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

    /**
     * As soon as a response is received from the server this method is called to display the message on the screen
     * @param messageID messageID returned from the server corresponding to the sent message
     * @param messageText
     */
    private void addMessageToScreen(String messageID,String messageText)
    {
        if(messageIDSet.contains(messageID))
        {
            return ;
        }
        messageIDSet.add(messageID);
        lastMessageID = messageID;
        findViewById(R.id.view_message_constraint_layout).requestFocus();
        List <Message> newMessage = new ArrayList<Message>();
        newMessage.add(new Message(messageID,chatID,false,messageText,Long.toString(System.currentTimeMillis()),null,null,null));
        messageRecyclerAdapter.addMessages(newMessage);
        recyclerMessages.smoothScrollToPosition(recyclerMessages.getAdapter().getItemCount()-1);
    }

    /**
     * launches the file explorer to pick any file to send.
     */
    private void pickFile()
    {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE);
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

    /**
     * This method parses the jsonObject and extracts all the required parameters to build a message object.
     * @param message jsonObject received from the server
     * @return
     * @throws JSONException
     */
    private Message jsonToMessage(JSONObject message) throws JSONException
    {
        String messageID = message.getString("MessageId");
        String chatID = message.getString("ChatId");
        boolean received = !message.getBoolean("SentByCurrentUser");
        JSONObject sendTime = message.getJSONObject("SentTs");
        String seconds = sendTime.getString("seconds");
        String text = null;
        String filename = null;
        String mimetype = null;
        String filesize = null;

        if(message.has("TextContent"))
        {
            text = message.getString("TextContent");
        }
        if(message.has("FileName"))
        {
            filename = message.getString("FileName");
        }
        if(message.has("FileType"))
        {
            mimetype = message.getString("FileType");
        }
        if(message.has("FileSize"))
        {
            filesize = message.getString("FileSize");
        }
        return new Message(messageID,chatID,received,text,
                seconds+"000",filename,mimetype,filesize);
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
            case R.id.menu_send_file:
                pickFile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public String getFileName(Uri uri)
    {
        String result = null;
        if (uri.getScheme().equals("content"))
        {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try
            {
                if (cursor != null && cursor.moveToFirst())
                {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
            finally
            {
                cursor.close();
            }
        }
        if (result == null)
        {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1)
            {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    /**
     * returns the mimeType of the uri provided
     * @param uri
     * @return
     */
    public String getMimeType(Uri uri)
    {
        String mimeType = null;
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT))
        {
            ContentResolver cr = getApplicationContext().getContentResolver();
            mimeType = cr.getType(uri);
        }
        else
        {
            String fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
        }
        return mimeType;
    }
    class UpdateCache extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground(String... strings)
        {
            String messageID = strings[0];
            String filePath = strings[1];
            OpenHelper dbOpenHelper = new OpenHelper(ViewMessageActivity.this);
            DataManager.loadToDatabase(dbOpenHelper,messageID,filePath);
            dbOpenHelper.close();
            return null;
        }
    }

    /**
     * After selection of a file from the file explorer this dialog is shown to send richText.
     * @throws IOException
     */
    public void showFileDialog() throws IOException
    {
        final Dialog builder = new Dialog(this, android.R.style.Theme_Light);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Objects.requireNonNull(builder.getWindow()).setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));

        builder.setContentView(R.layout.dialog);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView= inflater.inflate(R.layout.dialog, null);
        builder.setContentView(dialogView);
        builder.getWindow().setBackgroundDrawableResource(android.R.color.black);
        if(getMimeType(fileUri).startsWith("image"))//file is an image
        {
            ImageView imageView = dialogView.findViewById(R.id.send_image);
            imageView.setImageURI(fileUri);
            imageView.setVisibility(View.VISIBLE);
        }
        else
        {
            TextView fileNameLabel = dialogView.findViewById(R.id.file_name_text_view);
            TextView fileName = dialogView.findViewById(R.id.file_name);
            TextView fileSizeLabel = dialogView.findViewById(R.id.file_size_text_view);
            TextView fileSize = dialogView.findViewById(R.id.file_size);

            fileNameLabel.setVisibility(View.VISIBLE);
            fileName.setVisibility(View.VISIBLE);
            fileSizeLabel.setVisibility(View.VISIBLE);
            fileSize.setVisibility(View.VISIBLE);

            AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(fileUri,"r");
            assert afd != null;
            long size = afd.getLength();
            afd.close();

            fileName.setText(getFileName(fileUri));
            fileSize.setText(Long.toString(size) + " bytes");
        }
        Button sendButton = dialogView.findViewById(R.id.send_message_button);
        Button closeButton = dialogView.findViewById(R.id.close_button);
        sendButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                sendFileToServer();
                builder.dismiss();
            }
        });
        closeButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                builder.dismiss();
            }
        });
        builder.setOnKeyListener(new Dialog.OnKeyListener()
        {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
            {
                if (keyCode == KeyEvent.KEYCODE_BACK)
                {
                    builder.dismiss();
                }
                return true;
            }
        });
        builder.show();
    }

    private void loadChatIDFromServer() throws JSONException
    {
        String URL = BASE_URL + USERS + "/"
                + currentUser + "/" + CHATS;

        JSONObject jsonBody = new JSONObject();
        Log.d("username sent to server: ",username);
        jsonBody.put("username",username);

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
                                chatID = response.getString("ChatId");
                                getLastMessageID(chatID);
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
                        handleError(error);
                    }
                }) {
            @Override
            public Priority getPriority()
            {
                return Priority.IMMEDIATE;
            }
        };
        VolleyController.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }

    private void getLastMessageID(String chatID)
    {
        String URL = BASE_URL + USERS + "/"
                + currentUser + "/" + CHATS + "/" + chatID;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, URL, null, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        Log.d("ResponseMessage: " , response.toString());
                        try
                        {
                            lastMessageID = (response.getJSONObject("payload")).getString("LastSentMessageId");
                            firstReceiveMessageFromServer();
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
                        handleError(error);
                    }
                }) {
            @Override
            public Priority getPriority()
            {
                return Priority.IMMEDIATE;
            }
        };
        VolleyController.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }
    private void handleError(VolleyError error)
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
}