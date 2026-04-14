# shadow_dimension_java

## Google Login / Signup Setup

The app now supports `Continue with Google` from the Connect Soul page.

### 1) Create Google OAuth credentials (Desktop)

1. In Google Cloud Console, create an OAuth 2.0 Client ID for Desktop App.
2. Keep this client as `Desktop App` (Application de bureau).

The app uses a local callback on:

`http://127.0.0.1:8765`

### 2) Set environment variables (Windows PowerShell)

```powershell
setx GOOGLE_CLIENT_ID "your-google-client-id"
```

Close and reopen VS Code after running `setx` so variables are visible to Java.

### 3) Ensure the user table has a google_id column

```sql
ALTER TABLE `user`
ADD COLUMN `google_id` VARCHAR(255) NULL UNIQUE;
```

### 4) Run the app

On Connect Soul, click `Continue with Google`.
If the Google email already exists in your database, the account is linked automatically.
Otherwise, a new user is created with default role `ROLE_USER`.

## Email Verification On Signup

When a user signs up with email/password, a verification code is sent to the same email address.
The account can login only after code verification.

### SMTP environment variables (Windows PowerShell)

```powershell
setx MAIL_USERNAME "your-smtp-username"
setx MAIL_PASSWORD "your-smtp-password"
setx MAIL_FROM "your-from-email@example.com"
setx MAIL_SMTP_HOST "smtp.gmail.com"
setx MAIL_SMTP_PORT "587"
```

Restart VS Code after setting these variables.

### Verification flow

1. Fill signup form and click `Create Account`.
2. Enter the received 6-digit code.
3. Click `Verify Email`.
4. Then login normally.

Use `Resend Code` if the code expired (10 minutes).