package com.gpayinterns.chat;


import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.gpayinterns.chat.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.gpayinterns.chat.Message;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static androidx.core.content.FileProvider.getUriForFile;
import static com.gpayinterns.chat.ServerConstants.BASE_URL;
import static com.gpayinterns.chat.ServerConstants.CHATS;
import static com.gpayinterns.chat.ServerConstants.END_MESSAGE;
import static com.gpayinterns.chat.ServerConstants.MESSAGES;
import static com.gpayinterns.chat.ServerConstants.USERS;

public class MessageRecyclerAdapter extends RecyclerView.Adapter <MessageRecyclerAdapter.ViewHolder>
{
    private final Context mContext;//stores the context of ViewMessageActivity
    private final LayoutInflater mLayoutInflater;
    private List<Message> mMessages;//stores all messages which are displayed in the view.
    private int mViewType;
    String currentUser;

    //mViewType 0: left side text
    //mViewType 1: right side text
    //mViewType 2: left side richText
    //mViewType 3: right side richText

    /**
     * This method
     *
     * @param position
     * @return         viewType according to the position
     */
    @Override
    public int getItemViewType(int position)
    {
        if(mMessages.get(position).received)//Message is received
        {
            mViewType = 0;
        }
        else
        {
            mViewType = 1;
        }

        if(mMessages.get(position).mimeType!=null)//richText
        {
            mViewType += 2;
        }
        return mViewType;
    }

    /**
     *
     * @param Context      context of ViewMessageActivity
     * @param messages     list of messages corresponding to the chatID
     * @param mCurrentUser userID of the user who is logged in
     */
    public MessageRecyclerAdapter(Context Context, List <Message> messages, String mCurrentUser)
    {
        mContext = Context;
        mMessages = messages;
        mLayoutInflater = LayoutInflater.from(mContext);
        currentUser = mCurrentUser;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View itemView;
        switch(viewType)
        {
            case 0:
                itemView = mLayoutInflater.inflate(R.layout.item_recieve_message_list,parent,false);
                break;
            case 1:
                itemView = mLayoutInflater.inflate(R.layout.item_send_message_list,parent,false);
                break;
            case 2:
                itemView = mLayoutInflater.inflate(R.layout.item_recieve_richtext_list,parent,false);
                break;
            case 3:
                itemView = mLayoutInflater.inflate(R.layout.item_send_richtext_list,parent,false);
                break;
            default:
                itemView = null;
        }
        assert itemView != null;
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        holder.mMessageID = mMessages.get(position).messageID;
        holder.mChatID = mMessages.get(position).chatID;
        holder.mMimeType = mMessages.get(position).mimeType;
        if(mViewType<=1)//text
        {
            holder.mMessage.setText(mMessages.get(position).text);
        }
        else //richText
        {
            holder.mFileName.setText(mMessages.get(position).fileName);
            holder.mFileSize.setText(mMessages.get(position).fileSize);
        }
        holder.mTime.setText(convertDate(mMessages.get(position).sendTime));
    }


    @Override
    public int getItemCount()
    {
        return mMessages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {
        public final TextView mMessage;
        public final TextView mTime;
        public final TextView mFileName;
        public final TextView mFileSize;
        public final Button mDownloadButton;
        public final Button mViewButton;
        public String mMessageID;
        public String mChatID;
        public String mMimeType;

        public ViewHolder(@NonNull final View itemView)
        {
            super(itemView);

            mDownloadButton = (Button) itemView.findViewById(R.id.download_button);
            mViewButton = (Button) itemView.findViewById(R.id.view_button);
            if(mViewType%2==0)//received
            {
                mMessage = (TextView) itemView.findViewById(R.id.receive_message_text);
                mTime = (TextView) itemView.findViewById(R.id.time_receive_message_text);
                mFileName = (TextView) itemView.findViewById(R.id.receive_file_name);
                mFileSize = (TextView) itemView.findViewById(R.id.receive_file_size);
            }
            else
            {
                mMessage = (TextView) itemView.findViewById(R.id.send_message_text);
                mTime = (TextView) itemView.findViewById(R.id.time_send_message_text);
                mFileName = (TextView) itemView.findViewById(R.id.send_file_name);
                mFileSize = (TextView) itemView.findViewById(R.id.send_file_size);
            }

            /*
             * when download button is clicked it checks for the existence of path in the cache.
             * If it already exists, it doesn't download it again.
             * else getAttachmentFromServer() is called to download it.
             */
            if(mDownloadButton!=null)
            {
                mDownloadButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        ProgressBar progressBar = (ProgressBar) itemView.findViewById(R.id.progress_bar);
                        ImageView done = (ImageView) itemView.findViewById(R.id.done);
                        if (fileExists(getPath(mMessageID)))
                        {
                            Toast toast = Toast.makeText(mContext, "File is already downloaded", Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
                            toast.show();
                        }
                        else
                        {
                            progressBar.setVisibility(View.VISIBLE);
                            getAttachmentFromServer(mChatID, mMessageID, mFileName.getText().toString(), progressBar, done);
                        }
                    }
                });
            }

            /*
             * Upon clicking the view button, the path stored in the cache is checked for validity.
             * if it is valid, the file is shown to the user.
             * else the user is asked to download the file once again.
             */
            if(mViewButton!=null)
            {
                mViewButton.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        String path = getPath(mMessageID);
                        if(fileExists(path))
                        {
                            Uri messageURI = getUriForFile(mContext, "com.gpayinterns.fileprovider", new File (path));
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.setDataAndType(messageURI,mMimeType);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            mContext.startActivity(intent);
                        }
                        else
                        {
                            Toast toast = Toast.makeText(mContext, "File not present in cache.\nDownload it.", Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
                            toast.show();
                        }
                    }
                });
            }
        }
    }

    /**
     * Adds the messages present in newMessages to the list mMessages
     * @param newMessages  list of messages which is to be added to the back of the list 'mMessages'.
     */
    public void addMessages(List<Message> newMessages)
    {
        int positionStart = mMessages.size();

        //this if-else block is necessary as notifyItemRangeInserted() causes errors when the range is of length 1.
        if(newMessages.size()>1)
        {
            mMessages.addAll(newMessages);
            notifyItemRangeInserted(positionStart, newMessages.size());
        }
        else
        {
            mMessages.addAll(newMessages);
            notifyItemInserted(positionStart);
        }
    }

    /**
     * messages present in newMessages is added to the beginning of mMessages,
     * this method is used when the user scrolls up & hits the end.
     *
     * @param newMessages  list of messages which is to be added to the front of the list 'mMessages'.
     */
    public void addMessagesToFront(List<Message> newMessages)
    {
        //this if-else block is necessary as notifyItemRangeInserted() causes errors when the range is of length 1.
        if(newMessages.size()>1)
        {
            for(int i=newMessages.size()-1;i>=0;i--)
            {
                mMessages.add(0,newMessages.get(i));
            }
            notifyItemRangeInserted(0, newMessages.size());
        }
        else
        {
            mMessages.add(0,newMessages.get(0));
            notifyItemInserted(0);
        }
    }

    public static String convertDate(String dateInMilliseconds)
    {
        // 6 extra spaces are added just for formatting purposes
        return "      "+DateFormat.format("hh:mm a", Long.parseLong(dateInMilliseconds)).toString();
    }

    /**
     * @param path         the path which is to be checked for validity
     * @return             true if the path obtained is valid, else false
     */
    private boolean fileExists(String path)
    {
        if(path == null)
        {
            return false;
        }
        File file = new File(path);
        return file.exists();
    }

    /**
     * @param messageID    messageID corresponding to which the path stored has to be returned
     * @return             the path in phone storage corresponding to the attachment.
     */
    private String getPath(String messageID)
    {
        String path = null;
        OpenHelper dbOpenHelper = new OpenHelper(mContext);
        path = DataManager.getFromDatabase(dbOpenHelper,messageID);
        dbOpenHelper.close();
        return path;
    }

    /**
     *
     * @param chatID       the chatID between which the message was exchanged
     * @param messageID    messageID corresponding to the attachment
     * @param fileName     name of the file
     * @param progressBar  this progressbar helps the user to see that the file is being downloaded
     * @param done         this image gets displayed for a couple of seconds when the file is downloaded.
     */
    private void getAttachmentFromServer(String chatID, final String messageID, final String fileName,
                                         final ProgressBar progressBar, final ImageView done)
    {

        String URL = BASE_URL + USERS +
                "/" + currentUser + "/" +
                CHATS + "/" + chatID + "/" +
                MESSAGES + "/" + messageID + "/attachments";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, URL, null, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        progressBar.setVisibility(View.GONE);
                        Log.d("ResponseMessage:" , response.toString());
                        try
                        {
                            String base64String = response.getString("Blob");
                            String fileType = response.getString("FileType");
                            Log.d("base64",base64String);
                            storeFile(messageID,base64String,fileName);
                            done.setVisibility(View.VISIBLE);
                            new CountDownTimer(2000, 1000)
                            {
                                @Override
                                public void onTick(long millisUntilFinished)
                                {
                                }
                                @Override
                                public void onFinish()
                                {
                                    done.setVisibility(View.GONE);
                                }
                            }.start();
                        }
                        catch (JSONException | IOException e)
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

        VolleyController.getInstance(mContext).addToRequestQueue(jsonObjectRequest);
    }

    /**
     * This method helps store the file into the phone storage.
     *
     * @param messageID    messageID of the message whose file is to be stored
     * @param base         the file which is to br stored in a base64 format received from server
     * @param fileName     the name of the file
     * @throws IOException
     */
    private void storeFile(String messageID,String base, String fileName) throws IOException
    {
        String filePath = mContext.getFilesDir().getAbsolutePath() + "/" + fileName;
        FileOutputStream fos = new FileOutputStream(filePath);
        fos.write(Base64.decode(base,Base64.NO_WRAP));
        fos.close();

        updateCache(messageID,filePath);
        Log.d("path","file saved to:"+filePath);
    }

    /**
     * This method stores (messageID,path) in an SQLite DB, so that the data is persistent
     * and only uninstalling the application can lead to its deletion.
     *
     * @param messageID    messageID of the message whose file is to be stored
     * @param filePath     the path where the file has been stored
     */
    private void updateCache(String messageID, String filePath)
    {
        OpenHelper dbOpenHelper = new OpenHelper(mContext);
        DataManager.loadToDatabase(dbOpenHelper,messageID,filePath);
        dbOpenHelper.close();
    }
}