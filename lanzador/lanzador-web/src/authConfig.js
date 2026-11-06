// Configuración MSAL (misma base que la EP1), con los IDs en .env
export const msalConfig = {
  auth: {
    clientId: process.env.REACT_APP_CLIENT_ID,
    authority: `https://login.microsoftonline.com/${process.env.REACT_APP_TENANT_ID}`,
    redirectUri: process.env.REACT_APP_REDIRECT_URI || "http://localhost:3000/",
  },
  cache: { cacheLocation: "sessionStorage" },
};

export const loginRequest = { scopes: ["openid", "profile", "email"] };

export const apiRequest = { scopes: [process.env.REACT_APP_API_SCOPE] };
