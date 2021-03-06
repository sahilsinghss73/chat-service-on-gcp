package com.gpayinterns.chat;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.ClientError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.gpayinterns.chat.ServerConstants.BASE_URL;
import static com.gpayinterns.chat.ServerConstants.CHATS;
import static com.gpayinterns.chat.ServerConstants.MESSAGES;
import static com.gpayinterns.chat.ServerConstants.USERS;

public class NewMessageActivity extends AppCompatActivity implements View.OnClickListener
{
    EditText messageEditText;
    NewMessageRecyclerAdapter messageRecyclerAdapter;
    private RecyclerView recyclerMessages;
    LinearLayoutManager messageLayoutManager;
    private String currentUser;
    private List<String> messages = new ArrayList<String>();
    private static final int CONTACT_PICKER_RESULT = 1;
    public static final int PERMISSIONS_REQUEST_READ_CONTACTS = 1;
    private String chatID;
    private String messageText;
    private static boolean active;

    /**
     * This Activity helps to start a conversation with a user whose username
     * will be provided in an editText at the top of the screen.
     *
     * The user also gets an option to scroll through the contacts present on the device
     * and choose a contact to have a conversation with.
     *
     * The user has to perform the following actions to start a chat:
     * 1. Enter/Select a valid username.
     * 2. Enter a message to send & tap on send button
     *
     *
     * This activity performs it's task in the following manner:
     * 1. Get the chatID with the username provided using createChat
     * 2. With the received chatID send the message to the server
     * 3. Switch to ViewMessage Activity once the message has been sent.
     */
    @Override
    protected void onResume()
    {
        active=true;
        super.onResume();
    }

    @Override
    protected void onPause()
    {
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        active=false;
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_message);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        messageEditText=(EditText)findViewById(R.id.send_new_message_text);
        findViewById(R.id.send_new_message_button).setOnClickListener(this);
        findViewById(R.id.view_contacts_button).setOnClickListener(this);

        chatID = null;
        initializeDisplayContent();
        getCurrentUser();
    }

    private void initializeDisplayContent()
    {
        recyclerMessages = (RecyclerView) findViewById(R.id.new_message_recyclerView);
        messageLayoutManager = new LinearLayoutManager(this);
        messageRecyclerAdapter = new NewMessageRecyclerAdapter(this,messages);
        recyclerMessages.setLayoutManager(messageLayoutManager);
        recyclerMessages.setAdapter(messageRecyclerAdapter);
    }

    private void getCurrentUser()
    {
        SharedPreferences mPrefs= getSharedPreferences("CHAT_LOGGED_IN_USER", 0);
        currentUser = mPrefs.getString("currentUser","");
    }

    @Override
    public void onClick(View v)
    {
        if(v.getId()==R.id.view_contacts_button)
        {
            ((EditText)findViewById(R.id.new_message_username)).setText("");
            requestContactPermission();
        }
        else if(v.getId() == R.id.send_new_message_button)
        {
            try
            {
                messageText = messageEditText.getText().toString().trim();
                if(messageText.equals(""))
                    return;
                getChatID();
                messages.add(messageText);
                messageRecyclerAdapter.notifyItemInserted(messages.size()-1);
                recyclerMessages.smoothScrollToPosition(messages.size()-1);
                messageEditText.setText("");
                hideSoftKeyboard();
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        if (resultCode == RESULT_OK && requestCode == CONTACT_PICKER_RESULT)
        {
            Cursor cursor = null;
            String email = "", name = "";
            try
            {
                assert data != null;
                Uri result = data.getData();
                Log.v("NewMessageActivity", "Got a contact result: " + result.toString());

                String id = ((Uri) result).getLastPathSegment();

                cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,  null, ContactsContract.CommonDataKinds.Email.CONTACT_ID+" =? ", new String[] {id}, null);

                assert cursor != null;
                int nameId = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);

                int emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);

                if (cursor.moveToFirst())
                {
                    email = cursor.getString(emailIdx);
                    name = cursor.getString(nameId);
                    Log.v("NewMessageActivity", "Got email: " + email);
                }
                else
                {
                    Log.w("NewMessageActivity", "No results");
                }
            }
            catch (Exception e)
            {
                Log.e("NewMessageActivity", "Failed to get email data", e);
            }
            finally
            {
                if (cursor != null)
                {
                    cursor.close();
                }
                EditText emailEntry = (EditText) findViewById(R.id.new_message_username);
                Log.d("NewMessageActivity",email);
                Log.d("NewMessageActivity",name);
                emailEntry.setText(email);
                if (email.length() == 0 && name.length() == 0)
                {
                    Toast.makeText(this, "No Email found for selected contact",Toast.LENGTH_LONG).show();
                }
            }
        }
        else
        {
            Log.d("NewMessageActivity", "Warning: Activity result not ok");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void getChatID() throws JSONException
    {
        String URL = BASE_URL + USERS + "/"
                + currentUser + "/" + CHATS;

        String username = ((EditText)findViewById(R.id.new_message_username)).getText().toString();
        JSONObject jsonBody = new JSONObject();
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
                                sendFirstMessageToServer(messageText);
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
                            String message = data.optString("message");
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                            messages.clear();
                            messageRecyclerAdapter.notifyDataSetChanged();
                        }
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

    private void sendFirstMessageToServer(final String messageText)
    {
        String URL = BASE_URL + USERS
                + "/" + currentUser + "/" + CHATS
                + "/" + chatID + "/" + MESSAGES;

        Log.d("message sent to server: ",messageText);

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
                            String message = result.getString("message");
                            if(message.equals("Success"))
                            {
                                String messageID = result.getString("MessageId");
                                switchToViewMessages(messageID);
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

    private void switchToViewMessages(String lastMessageID)
    {
        if(active)
        {
            Intent intent = new Intent(this, ViewMessageActivity.class);

            String username = ((EditText) findViewById(R.id.new_message_username)).getText().toString();

            intent.putExtra(ViewMessageActivity.CHAT_ID, chatID);
            intent.putExtra(ViewMessageActivity.CONTACT_USERNAME, username);
            intent.putExtra(ViewMessageActivity.LAST_MESSAGE_ID, lastMessageID);

            startActivity(intent);
            finish();
        }
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

    //explicitly request to access contacts if permission not already provided
    public void requestContactPermission()
    {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.READ_CONTACTS))
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Read Contacts permission");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setMessage("Please enable access to contacts.");
                builder.setOnDismissListener(new DialogInterface.OnDismissListener()
                {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialog)
                    {
                        requestPermissions(
                                new String[]
                                        {android.Manifest.permission.READ_CONTACTS}
                                , PERMISSIONS_REQUEST_READ_CONTACTS);
                    }
                });
                builder.show();
            }
            else
            {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_CONTACTS},
                        PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        }
        else
        {
            getContacts();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        if(requestCode == PERMISSIONS_REQUEST_READ_CONTACTS)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                getContacts();
            }
            else
            {
                Toast.makeText(this, "You have disabled a contacts permission", Toast.LENGTH_LONG).show();
            }
        }
    }
    //launches the contacts application to select a contact
    private void getContacts()
    {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
    }
}