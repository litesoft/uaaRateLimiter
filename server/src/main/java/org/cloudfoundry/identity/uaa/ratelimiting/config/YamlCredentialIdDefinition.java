package org.cloudfoundry.identity.uaa.ratelimiting.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.cloudfoundry.identity.uaa.ratelimiting.core.config.exception.RateLimitingConfigException;
import org.cloudfoundry.identity.uaa.ratelimiting.util.StringUtils;

@Getter
@ToString
@RequiredArgsConstructor
public class YamlCredentialIdDefinition {
    public static final String EMPTY_KEY_FROM_PREFIX = "Empty key from: ";

    private final String key;
    private final String postKeyConfig;

    /**
     * Map a yaml CredentialID (String) to <code>key</code> & <code>postKeyConfig</code> pair into a <code>YamlCredentialIdDefinition</code>.
     *
     * @return null if <code>yamlCredentialID</code> is null or blank.
     */
    public static YamlCredentialIdDefinition from( String yamlCredentialID ) {
        yamlCredentialID = StringUtils.normalizeToNull( yamlCredentialID );
        if ( yamlCredentialID == null ) {
            return null;
        }
        String key = yamlCredentialID;
        String postKeyConfig = null;
        int at = yamlCredentialID.indexOf( ':' ); // key and postKeyConfig separator
        if ( at != -1 ) {
            key = StringUtils.normalizeToNull( yamlCredentialID.substring( 0, at ) );
            postKeyConfig = StringUtils.normalizeToNull( yamlCredentialID.substring( at + 1 ) );
            if ( key == null ) {
                throw new RateLimitingConfigException( EMPTY_KEY_FROM_PREFIX + yamlCredentialID );
            }
        }
        return new YamlCredentialIdDefinition( key, postKeyConfig );
    }
}
