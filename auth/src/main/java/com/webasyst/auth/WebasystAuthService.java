package com.webasyst.auth;

import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.CodeVerifierUtil;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class WebasystAuthService {
    private static final String TAG = "WA_AUTH_SERVICE";

    private static final String DEVICE_ID_PARAM = "device_id";

    private final WebasystAuthStateStore stateStore;
    public final WebasystAuthConfiguration configuration;
    public final AuthorizationServiceConfiguration authServiceConfiguration;
    private final AuthorizationService authorizationService;

    private final Map<String, String> additionalParams;

    @Nullable
    static WebasystAuthConfiguration currentConfiguration = null;

    private WebasystAuthService(@NonNull final Context context,
                                @NonNull final WebasystAuthConfiguration configuration) {
        stateStore = WebasystAuthStateStore.getInstance(context);
        this.configuration = configuration;
        this.authServiceConfiguration = new AuthorizationServiceConfiguration(
            configuration.authEndpoint,
            configuration.tokenEndpoint
        );
        authorizationService = new AuthorizationService(context);
        additionalParams = new HashMap();
        additionalParams.put(DEVICE_ID_PARAM, configuration.deviceId);
    }

    public static WebasystAuthService getInstance(@NonNull final Context context) {
        if (null == currentConfiguration) {
            throw new IllegalStateException("Configuration must be set");
        }
        return new WebasystAuthService(context, currentConfiguration);
    }

    private AuthorizationService getAuthorizationService() {
        return authorizationService;
    }

    /**
     * Configures WAID service. This method should be called once, before any interactions with WAID.
     *
     * The recommended point to do it is your Application's onCreate() method
     * (if you use custom Application class) or authentication activity's onCreate().
     *
     * @param configuration Configuration object
     */
    public static void configure(WebasystAuthConfiguration configuration) {
        currentConfiguration = configuration;
    }

    void performTokenRequest(TokenRequest request) {
        TokenRequest actualRequest = new TokenRequest
            .Builder(request.configuration, request.clientId)
            .setAdditionalParameters(additionalParams)
            .setGrantType(request.grantType)
            .setRedirectUri(request.redirectUri)
            .setScope(request.scope)
            .setAuthorizationCode(request.authorizationCode)
            .setRefreshToken(request.refreshToken)
            .setCodeVerifier(request.codeVerifier)
            .build();
        authorizationService.performTokenRequest(actualRequest, stateStore::updateAfterTokenResponse);
    }

    /**
     * Performs given task with fresh access token. Token is automatically refreshed if needed.
     */
    public <T> void withFreshAccessToken(final AccessTokenTask<T> task) {
        withFreshAccessToken(task, null);
    }

    /**
     * Performs given task with fresh access token. Token is automatically refreshed if needed.
     * This variant if {@link #withFreshAccessToken} calls the callback upon task completion.
     */
    public <T> void withFreshAccessToken(final AccessTokenTask<T> task, @Nullable final Consumer<T> callback) {
        Log.d(TAG, "Running task with fresh token...");
        final AuthState authState = stateStore.getCurrent();
        AuthorizationException authorizationException = null;
        try {
            withFreshAccessToken(authState, task, callback);
        } catch (AuthorizationException exception) {
            authorizationException = exception;
        } finally {
            if (null != authorizationException) {
                Log.w(TAG, "Caught exception in withFreshToken()", authorizationException);
                if (authorizationException.code >= 2000) {
                    stateStore.replace(new AuthState());
                } else {
                    stateStore.writeCurrent();
                }
            } else {
                stateStore.writeCurrent();
            }
        }
    }

    public <T> void withFreshAccessToken(final AuthState authState, final AccessTokenTask<T> task, @Nullable final Consumer<T> callback) throws AuthorizationException {
        AtomicReference<AuthorizationException> authorizationExceptionRef = new AtomicReference<>(null);
        authState.performActionWithFreshTokens(
            authorizationService,
            additionalParams,
            (accessToken, idToken, exception) -> {
                authorizationExceptionRef.set(exception);
                final T result = task.apply(accessToken, exception);
                if (null != callback) callback.accept(result);
            }
        );
        final AuthorizationException authorizationException = authorizationExceptionRef.get();
        if (null != authorizationException) {
            throw authorizationException;
        }
    }

    /**
     * Performs sign in
     *
     * @param request Sign in request
     * @param success {@link PendingIntent} to be called upon successful sign in
     * @param cancelled {@link PendingIntent} to be called upon sign in cancellation
     */
    public void signIn(AuthorizationRequest request, PendingIntent success, PendingIntent cancelled) {
        final AuthorizationRequest actualRequest = new AuthorizationRequest
            .Builder(request.configuration, request.clientId, request.responseType, request.redirectUri)
            .setAdditionalParameters(additionalParams)
            .setDisplay(request.display)
            .setLoginHint(request.loginHint)
            .setPrompt(request.prompt)
            .setScope(request.scope)
            .setState(request.state)
            .setCodeVerifier(request.codeVerifier, request.codeVerifierChallenge, request.codeVerifierChallengeMethod)
            .setResponseMode(request.responseMode)
            .build();
        getAuthorizationService().performAuthorizationRequest(actualRequest, success, cancelled);
    }

    /**
     * Performs sign out
     */
    public void signOut() {
        stateStore.replace(new AuthState());
    }

    /**
     * Releases resources
     */
    public void dispose() {
        authorizationService.dispose();
    }

    AuthorizationRequest createAuthorizationRequest() {
        final String codeVerifier = CodeVerifierUtil.generateRandomCodeVerifier();

        return new AuthorizationRequest.Builder(
            authServiceConfiguration,
            configuration.clientId,
            ResponseTypeValues.CODE,
            configuration.callbackUri
        )
            .setCodeVerifier(codeVerifier)
            .setScopes(configuration.scope)
            .build();
    }

    /**
     * This interface represents task to be used with {@link #withFreshAccessToken}.
     *
     * @param <T>
     */
    public interface AccessTokenTask<T> {
        T apply(@Nullable String accessToken, @Nullable Exception exception);
    }
}
