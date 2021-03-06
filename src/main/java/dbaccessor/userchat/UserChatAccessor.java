package dbaccessor.userchat;

import entity.UserChat;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.cloud.spanner.Statement;
import org.springframework.cloud.gcp.data.spanner.core.SpannerQueryOptions;

/**
 * Accessor which performs database accesses for the UserChat entity.
 */
@Component
public final class UserChatAccessor {
    
    @Autowired
    private SpannerTemplate spannerTemplate;

    /**
     * Inserts all attributes of the given UserChat in the DB.
     */
    public void insertAll(UserChat userChat) {
        spannerTemplate.insert(userChat);
    }
    
    /**
     * Returns ChatId of the Chat between the given Users.
     * Returns an empty List if Chat does not exist between the given Users. 
     */
    public ImmutableList<UserChat> getChatIdIfChatExistsBetweenUserIds(long userId1, long userId2) {

        String sqlStatment = "SELECT ChatID FROM UserChat@{FORCE_INDEX=UserChatByUserID} WHERE UserID=@userId2 AND ChatID IN (SELECT ChatID FROM UserChat@{FORCE_INDEX=UserChatByUserID} WHERE UserID=@userId1)";
        Statement statement = Statement.newBuilder(sqlStatment).bind("userId2").to(userId2).bind("userId1").to(userId1).build();
        ImmutableList<UserChat> resultSet = ImmutableList.copyOf(spannerTemplate.query(UserChat.class, statement, new SpannerQueryOptions().setAllowPartialRead(true)));
    
        return resultSet;
    }

    /**
     * Checks if a UserChat entry for the given userId and chatId exists.
     */
    public boolean checkIfUserChatIdExists(long userId, long chatId) {

        String sqlStatment = "SELECT * FROM UserChat WHERE UserID=@userId AND ChatID=@chatId";
        Statement statement = Statement.newBuilder(sqlStatment).bind("userId").to(userId).bind("chatId").to(chatId).build();
        List<UserChat> resultSet = spannerTemplate.query(UserChat.class, statement, null);
    
        return !resultSet.isEmpty();
    }
}
