package service;

import com.mongodb.client.MongoCollection;
import exception.BadRequestException;
import model.user.Role;
import model.user.User;
import org.bson.types.ObjectId;
import repository.UserRepository;

import static api.Message.*;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static util.Constants.*;

public class UserService {

    private final UserRepository userRepository;

    public UserService(MongoCollection<User> collection) {
        userRepository = new UserRepository(collection);
    }

    public String create(User user) {
        validateUser(user);
        return userRepository.create(user);
    }

    public User authenticate(String encryptedEmail, String encryptedPassword) {
        User user = userRepository.findOne(and(eq(PASSWORD, encryptedPassword), eq(EMAIL, encryptedEmail)));
        if (user == null) {
            throw new BadRequestException(INVALID_USER_CREDENTIALS);
        }
        return user;
    }

    public boolean isAdmin(String authenticatedUser) {
        User user = userRepository.findOne(eq(EMAIL, authenticatedUser));
        if (user == null) {
            throw new BadRequestException(USER_NOT_FOUND);
        }
        return user.getRole().equals(Role.Admin);
    }

    public User get(String userId, String authenticatedUser) {
        User user = userRepository.get(new ObjectId(userId));
        if (!user.getEmail().equals(authenticatedUser)) {
            throw new BadRequestException(USERNAME_MISMATCH);
        }
        return user;
    }

    public User update(User userToUpdate, String authenticatedUser) {
        User user = userRepository.findOne(eq(DATABASE_ID, userToUpdate.getId()));
        if (user == null) {
            throw new BadRequestException(USER_NOT_FOUND);
        }
        if (!userToUpdate.getEmail().equals(authenticatedUser)) {
            throw new BadRequestException(USERNAME_MISMATCH);
        }
        return userRepository.update(userToUpdate);
    }

    private void validateUser(User user) {
        if (userRepository.exists(eq(EMAIL, user.getEmail()))) {
            throw new BadRequestException(USER_DUPLICATED_EMAIL);
        }
    }
}
