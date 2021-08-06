package top.fengpingtech.solen.app.auth;

public class SecurityContext {

    static final String HEADER_PRINCIPAL_NAME = "Authorization-Principal";

    private static final ThreadLocal<String> PRINCIPAL_HOLDER = new ThreadLocal<>();

    public static void setPrincipal(String principal) {
        PRINCIPAL_HOLDER.set(principal);
    }

    public static String getPrincipal() {
        return PRINCIPAL_HOLDER.get();
    }

    public static void clear() {
        PRINCIPAL_HOLDER.remove();
    }
}
