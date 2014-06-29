package org.royaldev.royalauth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

public enum Language {

    ADMIN_HELP,
    ADMIN_SET_UP_INCORRECTLY,
    ALREADY_LOGGED_IN,
    ALREADY_REGISTERED,
    ANOTHER_PLAYER_WITH_NAME,
    COMMAND_NO_CONSOLE,
    CONFIGURATION_RELOADED,
    CONTACT_ADMIN,
    CONVERTED_USERDATA,
    COULD_NOT_CONVERT_USERDATA,
    COULD_NOT_LOG_IN,
    COULD_NOT_REGISTER,
    COULD_NOT_REGISTER_COMMAND,
    COULD_NOT_START_METRICS,
    DISALLOWED_PASSWORD,
    ENABLED,
    ERROR,
    ERROR_OCCURRED,
    HAS_LOGGED_IN,
    HAS_REGISTERED,
    HELP_CHANGEPASSWORD,
    HELP_HELP,
    HELP_LOGIN,
    HELP_LOGOUT,
    HELP_REGISTER,
    HELP_RELOAD,
    INCORRECT_PASSWORD,
    INVALID_SUBCOMMAND,
    INVALID_USERNAME,
    LOGGED_IN_SUCCESSFULLY,
    LOGGED_IN_VIA_SESSION,
    LOGGED_OUT,
    METRICS_ENABLED,
    METRICS_OFF,
    NO_PERMISSION,
    NOT_ENOUGH_ARGUMENTS,
    NOT_LOGGED_IN,
    OLD_PASSWORD_INCORRECT,
    PASSWORD_CHANGED,
    PASSWORD_COULD_NOT_BE_CHANGED,
    PASSWORD_COULD_NOT_BE_SET,
    PASSWORD_SET_AND_REGISTERED,
    PLAYER_ALREADY_REGISTERED,
    PLAYER_LOGGED_IN,
    PLAYER_LOGGED_OUT,
    PLAYER_NOT_LOGGED_IN,
    PLAYER_NOT_ONLINE,
    PLAYER_NOT_REGISTERED,
    PLEASE_LOG_IN_WITH,
    PLEASE_REGISTER_WITH,
    REGISTERED_SUCCESSFULLY,
    TOOK_TOO_LONG_TO_LOG_IN,
    TRY,
    USED_INCORRECT_PASSWORD,
    WAS_LOGGED_IN_VIA_SESSION,
    YOU_MUST_LOGIN,
    YOUR_PASSWORD_CHANGED,
    YOUR_PASSWORD_COULD_NOT_BE_CHANGED;

    /**
     * Gets the message.
     *
     * @return Message
     */
    @Override
    public String toString() {
        return LanguageHelper.getString(name());
    }

    protected static class LanguageHelper {

        private static Properties p = new Properties();

        protected LanguageHelper(File f) throws IOException {
            final Reader in = new InputStreamReader(new FileInputStream(f), "UTF-8");
            LanguageHelper.p.load(in);
        }

        protected LanguageHelper(String s) throws IOException {
            final Reader in = new InputStreamReader(new FileInputStream(new File(s)), "UTF-8");
            LanguageHelper.p.load(in);
        }

        /**
         * Gets a property that is never null.
         *
         * @param node Node to get
         * @return String or "Language property "node" not defined."
         */
        private static String getString(String node) {
            String prop = p.getProperty(node);
            if (prop == null) prop = "Language property \"" + node + "\" not defined.";
            return prop;
        }
    }

}
