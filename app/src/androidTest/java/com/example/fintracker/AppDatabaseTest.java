package com.example.fintracker;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.fintracker.bll.validators.AccountValidator;
import com.example.fintracker.bll.validators.TagValidator;
import com.example.fintracker.bll.validators.UserValidator;
import com.example.fintracker.dal.local.dao.AccountDao;
import com.example.fintracker.dal.local.dao.TagDao;
import com.example.fintracker.dal.local.dao.UserDao;
import com.example.fintracker.dal.local.database.AppDatabase;
import com.example.fintracker.dal.local.entities.AccountEntity;
import com.example.fintracker.dal.local.entities.TagEntity;
import com.example.fintracker.dal.local.entities.UserEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Instrumented test for AppDatabase and related validators.
 * Uses an in-memory Room database for isolated, repeatable tests.
 */
@RunWith(AndroidJUnit4.class)
public class AppDatabaseTest {

    private AppDatabase database;
    private UserDao userDao;
    private AccountDao accountDao;
    private TagDao tagDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        // Create an in-memory database (data is cleared after each test)
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries() // Only for testing
                .build();

        userDao = database.userDao();
        accountDao = database.accountDao();
        tagDao = database.tagDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    /**
     * Helper method to generate consistent ISO 8601 timestamp strings.
     * Uses SQLite-compatible format: YYYY-MM-DD HH:MM:SS
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    // ========== USER VALIDATION AND AUTHENTICATION TESTS ==========

    @Test
    public void testInvalidEmailRejection() {
        try {
            UserValidator.isValidEmail("bad-email");
            fail("Expected IllegalArgumentException for invalid email");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("Email format is invalid"));
        }
    }

    @Test
    public void testValidEmailAcceptance() {
        assertTrue(UserValidator.isValidEmail("valid.email@example.com"));
    }

    @Test
    public void testUsernameWithWhitespace() {
        try {
            UserValidator.isValidUsername("  user  ");
            fail("Expected IllegalArgumentException for username with leading/trailing whitespace");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("cannot have leading or trailing whitespace"));
        }
    }

    @Test
    public void testUserRegistrationAndLogin() {
        String email = "test.user@example.com";
        String username = "testuser";
        String password = "password123";

        // Validate registration data
        assertTrue(UserValidator.validateRegistration(email, username, password));

        // Check that user doesn't exist
        assertFalse(userDao.checkIfUserExists(email, username));

        // Create and insert user
        UserEntity user = new UserEntity();
        user.id = UUID.randomUUID().toString();
        user.email = email;
        user.name = username;
        user.password = password;
        user.hourlyRate = 50.0;
        user.isBankSyncEnabled = true;
        user.isSynced = false;
        user.isDeleted = false;
        user.updatedAt = getCurrentTimestamp();

        userDao.insertUser(user);

        // Verify user exists
        assertTrue(userDao.checkIfUserExists(email, username));

        // Test login with email
        UserEntity loginWithEmail = userDao.getUserByEmailOrName(email, password);
        assertNotNull(loginWithEmail);
        assertEquals(user.id, loginWithEmail.id);

        // Test login with username
        UserEntity loginWithUsername = userDao.getUserByEmailOrName(username, password);
        assertNotNull(loginWithUsername);
        assertEquals(user.id, loginWithUsername.id);

        // Test login with wrong password
        UserEntity loginFailed = userDao.getUserByEmailOrName(email, "wrongpassword");
        assertNull(loginFailed);
    }

    @Test
    public void testDuplicateUserPrevention() {
        String email = "duplicate@example.com";
        String username = "duplicateuser";

        UserEntity user1 = new UserEntity();
        user1.id = UUID.randomUUID().toString();
        user1.email = email;
        user1.name = username;
        user1.password = "password123";
        user1.hourlyRate = 50.0;
        user1.isBankSyncEnabled = false;
        user1.isSynced = false;
        user1.isDeleted = false;
        user1.updatedAt = getCurrentTimestamp();

        userDao.insertUser(user1);

        // Check if user exists before attempting duplicate insertion
        assertTrue(userDao.checkIfUserExists(email, username));

        // In a real scenario, the app should prevent this insertion
        // For testing, we'll just verify the check works
    }

    // ========== ACCOUNT VALIDATION AND MANAGEMENT TESTS ==========

    @Test
    public void testNegativeBalanceRejection() {
        try {
            AccountValidator.isValidBalance(-100.0);
            fail("Expected IllegalArgumentException for negative balance");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("cannot be negative"));
        }
    }

    @Test
    public void testNaNBalanceRejection() {
        try {
            AccountValidator.isValidBalance(Double.NaN);
            fail("Expected IllegalArgumentException for NaN balance");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("NaN"));
        }
    }

    @Test
    public void testInfiniteBalanceRejection() {
        try {
            AccountValidator.isValidBalance(Double.POSITIVE_INFINITY);
            fail("Expected IllegalArgumentException for infinite balance");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("infinite"));
        }
    }

    @Test
    public void testAccountCreationAndRetrieval() {
        // Create test user
        String userId = createTestUser("account.test@example.com", "accounttester");

        // Validate account data
        String accountName = "Test Credit Card";
        double initialBalance = 5000.0;
        assertTrue(AccountValidator.validateAccountCreation(accountName, initialBalance));

        // Check if account already exists (idempotent operation)
        AccountEntity existingAccount = accountDao.getAccountByNameAndOwner(accountName, userId);
        if (existingAccount == null) {
            // Create new account
            AccountEntity account = new AccountEntity();
            account.id = UUID.randomUUID().toString();
            account.name = accountName;
            account.ownerId = userId;
            account.isShared = false;
            account.balance = initialBalance;
            account.isSynced = false;
            account.isDeleted = false;
            account.updatedAt = getCurrentTimestamp();

            accountDao.insertAccount(account);
        }

        // Retrieve accounts for user
        List<AccountEntity> accounts = accountDao.getAccountsByUserId(userId);
        assertFalse(accounts.isEmpty());
        assertEquals(1, accounts.size());
        assertEquals(accountName, accounts.get(0).name);
        assertEquals(initialBalance, accounts.get(0).balance, 0.01);
    }

    @Test
    public void testAccountBalanceUpdate() {
        // Create test user and account
        String userId = createTestUser("balance.test@example.com", "balancetester");
        String accountId = createTestAccount(userId, "Savings", 1000.0);

        // Update balance
        double newBalance = 1500.0;
        int rowsAffected = accountDao.updateAccountBalance(accountId, newBalance);
        assertEquals(1, rowsAffected);

        // Verify update
        AccountEntity updatedAccount = accountDao.getAccountById(accountId);
        assertNotNull(updatedAccount);
        assertEquals(newBalance, updatedAccount.balance, 0.01);
    }

    @Test
    public void testSoftDeletedAccountCannotBeUpdated() {
        // Create test user and account
        String userId = createTestUser("deleted.test@example.com", "deletedtester");
        String accountId = createTestAccount(userId, "Old Account", 500.0);

        // Soft-delete the account
        accountDao.deleteAccount(accountId);

        // Attempt to update balance on deleted account
        int rowsAffected = accountDao.updateAccountBalance(accountId, 1000.0);
        assertEquals(0, rowsAffected); // Should not update deleted account

        // Verify account is not visible in normal queries
        AccountEntity deletedAccount = accountDao.getAccountById(accountId);
        assertNull(deletedAccount);
    }

    @Test
    public void testIdempotentAccountCreation() {
        String userId = createTestUser("idempotent.test@example.com", "idempotentuser");
        String accountName = "Unique Account";

        // Create account first time
        String accountId1 = createTestAccountIdempotent(userId, accountName, 1000.0);

        // Attempt to create same account again (should return existing)
        String accountId2 = createTestAccountIdempotent(userId, accountName, 2000.0);

        // Should be the same account
        assertEquals(accountId1, accountId2);

        // Verify only one account exists
        List<AccountEntity> accounts = accountDao.getAccountsByUserId(userId);
        assertEquals(1, accounts.size());
    }

    // ========== TAG VALIDATION AND MANAGEMENT TESTS ==========

    @Test
    public void testInvalidTagNameRejection() {
        try {
            TagValidator.isValidTagName("");
            fail("Expected IllegalArgumentException for empty tag name");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("cannot be empty"));
        }
    }

    @Test
    public void testTagNameWithWhitespace() {
        try {
            TagValidator.isValidTagName("  Groceries  ");
            fail("Expected IllegalArgumentException for tag name with leading/trailing whitespace");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("cannot have leading or trailing whitespace"));
        }
    }

    @Test
    public void testTagCreationAndRetrieval() {
        // Create test user
        String userId = createTestUser("tag.test@example.com", "tagtester");

        // Validate tag data
        String tagName = "Groceries";
        assertTrue(TagValidator.validateTagCreation(tagName));

        // Check if tag already exists (idempotent operation)
        TagEntity existingTag = tagDao.getTagByNameAndOwner(tagName, userId);
        if (existingTag == null) {
            // Create new tag
            TagEntity tag = new TagEntity();
            tag.id = UUID.randomUUID().toString();
            tag.name = tagName;
            tag.iconName = "ic_groceries";
            tag.ownerId = userId;
            tag.isSynced = false;
            tag.isDeleted = false;
            tag.updatedAt = getCurrentTimestamp();

            tagDao.insertTag(tag);
        }

        // Retrieve tags for user
        List<TagEntity> tags = tagDao.getTagsByUserId(userId);
        assertFalse(tags.isEmpty());
        assertEquals(1, tags.size());
        assertEquals(tagName, tags.get(0).name);
    }

    @Test
    public void testIdempotentTagCreation() {
        String userId = createTestUser("tagidempotent.test@example.com", "tagidempotentuser");
        String tagName = "Entertainment";

        // Create tag first time
        String tagId1 = createTestTagIdempotent(userId, tagName);

        // Attempt to create same tag again (should return existing)
        String tagId2 = createTestTagIdempotent(userId, tagName);

        // Should be the same tag
        assertEquals(tagId1, tagId2);

        // Verify only one tag exists
        List<TagEntity> tags = tagDao.getTagsByUserId(userId);
        assertEquals(1, tags.size());
    }

    @Test
    public void testDefaultTagsRetrieval() {
        // Create a default tag (ownerId = null)
        TagEntity defaultTag = new TagEntity();
        defaultTag.id = UUID.randomUUID().toString();
        defaultTag.name = "Food";
        defaultTag.iconName = "ic_food";
        defaultTag.ownerId = null;
        defaultTag.isSynced = true;
        defaultTag.isDeleted = false;
        defaultTag.updatedAt = getCurrentTimestamp();

        tagDao.insertTag(defaultTag);

        // Retrieve default tags
        List<TagEntity> defaultTags = tagDao.getDefaultTags();
        assertFalse(defaultTags.isEmpty());
        assertTrue(defaultTags.stream().anyMatch(t -> "Food".equals(t.name)));
    }

    // ========== HELPER METHODS ==========

    /**
     * Helper method to create a test user and return the user ID.
     */
    private String createTestUser(String email, String username) {
        UserEntity user = new UserEntity();
        user.id = UUID.randomUUID().toString();
        user.email = email;
        user.name = username;
        user.password = "password123";
        user.hourlyRate = 50.0;
        user.isBankSyncEnabled = false;
        user.isSynced = false;
        user.isDeleted = false;
        user.updatedAt = getCurrentTimestamp();

        userDao.insertUser(user);
        return user.id;
    }

    /**
     * Helper method to create a test account and return the account ID.
     */
    private String createTestAccount(String userId, String accountName, double balance) {
        AccountEntity account = new AccountEntity();
        account.id = UUID.randomUUID().toString();
        account.name = accountName;
        account.ownerId = userId;
        account.isShared = false;
        account.balance = balance;
        account.isSynced = false;
        account.isDeleted = false;
        account.updatedAt = getCurrentTimestamp();

        accountDao.insertAccount(account);
        return account.id;
    }

    /**
     * Helper method for idempotent account creation.
     * Returns existing account ID if account already exists, otherwise creates new account.
     */
    private String createTestAccountIdempotent(String userId, String accountName, double balance) {
        AccountEntity existingAccount = accountDao.getAccountByNameAndOwner(accountName, userId);
        if (existingAccount != null) {
            return existingAccount.id;
        }
        return createTestAccount(userId, accountName, balance);
    }

    /**
     * Helper method for idempotent tag creation.
     * Returns existing tag ID if tag already exists, otherwise creates new tag.
     */
    private String createTestTagIdempotent(String userId, String tagName) {
        TagEntity existingTag = tagDao.getTagByNameAndOwner(tagName, userId);
        if (existingTag != null) {
            return existingTag.id;
        }

        TagEntity tag = new TagEntity();
        tag.id = UUID.randomUUID().toString();
        tag.name = tagName;
        tag.iconName = "ic_" + tagName.toLowerCase();
        tag.ownerId = userId;
        tag.isSynced = false;
        tag.isDeleted = false;
        tag.updatedAt = getCurrentTimestamp();

        tagDao.insertTag(tag);
        return tag.id;
    }
}








