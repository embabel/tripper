# Spring Security with Google OAuth2 Authentication

This application uses Spring Security with Google OAuth2 for authentication. Follow these steps to set up Google OAuth2:

## Setting Up Google OAuth2

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Navigate to "APIs & Services" > "Credentials"
4. Click "Create Credentials" and select "OAuth client ID"
5. Select "Web application" as the application type
6. Add a name for your OAuth client
7. Add authorized redirect URIs:
   - `http://localhost:8080/login/oauth2/code/google` (for local development)
   - Add your production URLs if deploying to production
8. Click "Create"
9. Google will provide a Client ID and Client Secret

## Configuring the Application

1. Open `src/main/resources/application.properties`
2. Replace the placeholder values with your actual Google OAuth2 credentials:

```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
```

## Security Configuration

The security configuration is defined in `SecurityConfig.kt`. The current setup:

- Requires authentication for all pages except static resources and the login page
- Uses Google OAuth2 for authentication
- Provides a login page at `/login`
- Redirects to the home page after successful login
- Allows logout with redirect to the login page

## User Information

After authentication, user details from Google are available:
- User profile at `/user` shows detailed information
- User name displayed in the navigation bar
- Access to OAuth2 user attributes in Thymeleaf templates

## Custom OAuth2 User Service

The application uses a custom OAuth2 user service (`CustomOAuth2UserService.kt`) to:
- Load user details from Google
- Extract user information (email, name)
- Assign default role (ROLE_USER)
- Return a properly configured OAuth2User

## Thymeleaf Security Integration

The application uses Thymeleaf's Spring Security integration to show/hide content based on authentication status:

- Use `sec:authorize="isAuthenticated()"` to show content only to authenticated users
- Use `sec:authentication="name"` to display the authenticated user's name

Example:
```html
<div sec:authorize="isAuthenticated()">
    Welcome, <span sec:authentication="name">User</span>!
</div>
```

## For Production Deployment

For production deployments, consider:
1. Enabling CSRF protection
2. Implementing proper user persistence in a database
3. Adding more granular authorization rules
4. Configuring secure session management