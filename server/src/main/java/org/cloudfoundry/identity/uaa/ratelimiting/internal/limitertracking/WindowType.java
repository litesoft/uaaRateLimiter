package org.cloudfoundry.identity.uaa.ratelimiting.internal.limitertracking;

import java.util.Map;
import java.util.function.Function;

import org.cloudfoundry.identity.uaa.ratelimiting.core.CompoundKey;
import org.cloudfoundry.identity.uaa.ratelimiting.core.config.LimiterMapping;
import org.cloudfoundry.identity.uaa.ratelimiting.core.config.RequestsPerWindowSecs;
import org.cloudfoundry.identity.uaa.ratelimiting.core.http.CallerIdSupplierByType;
import org.cloudfoundry.identity.uaa.ratelimiting.internal.common.InternalLimiterFactory;
import org.cloudfoundry.identity.uaa.ratelimiting.util.StringUtils;

public interface WindowType {
    String windowType();

    RequestsPerWindowSecs extractRequestsPerWindowFrom( LimiterMapping properties );

    String extractCallerIdFrom( CallerIdSupplierByType callerIdSupplier );

    /**
     * Get the Canned Caller ID (for the CompoundKey) - note: only valid on <code>GLOBAL</code> and <code>NON_GLOBAL.NoID</code>.
     *
     * @throws IllegalStateException if called on a WindowType that does not a canned Caller ID.
     */
    default String cannedCallerID() {
        throw new IllegalStateException( windowType() + " does not support a canned Caller ID" );
    }

    default boolean addTo( Map<CompoundKey, InternalLimiterFactory> map,
                           LimiterMapping properties, CallerIdSupplierByType callerIdSupplier ) {
        if ( properties != null ) {
            RequestsPerWindowSecs window = extractRequestsPerWindowFrom( properties );
            if ( window != null ) {
                String callerId = extractCallerIdFrom( callerIdSupplier );
                if ( callerId != null ) {
                    String limiterName = properties.name();
                    map.put( CompoundKey.from( limiterName, windowType(), callerId ),
                             InternalLimiterFactoryImpl.builder()
                                     .name( limiterName )
                                     .windowType( windowType() )
                                     .requestsPerWindow( window )
                                     .build() );
                    return true;
                }
            }
        }
        return false;
    }

    WindowType GLOBAL = new WindowType() {
        @Override
        public String windowType() {
            return "Global";
        }

        @Override
        public RequestsPerWindowSecs extractRequestsPerWindowFrom( LimiterMapping properties ) {
            return (properties == null) ? null : properties.global();
        }

        @Override
        public String extractCallerIdFrom( CallerIdSupplierByType callerIdSupplier ) {
            return cannedCallerID();
        }

        @Override
        public String cannedCallerID() {
            return "-Nope-";
        }
    };

    enum NON_GLOBAL implements WindowType {
        // Priority order for checking AND Locking
        CredentialsID( LimiterMapping::withCallerCredentialsID, CallerIdSupplierByType::getCallerCredentialsID ),
        RemoteAddressID( LimiterMapping::withCallerRemoteAddressID, CallerIdSupplierByType::getCallerRemoteAddressID ),
        NoID( LimiterMapping::withoutCallerID, null ) {
            @Override
            public String extractCallerIdFrom( CallerIdSupplierByType callerIdSupplier ) {
                return cannedCallerID();
            }

            @Override
            public String cannedCallerID() {
                return "-NoID-";
            }
        };

        private final Function<LimiterMapping, RequestsPerWindowSecs> windowMapper;
        private final Function<CallerIdSupplierByType, String> callerIdMapper;

        NON_GLOBAL( Function<LimiterMapping, RequestsPerWindowSecs> windowMapper,
                    Function<CallerIdSupplierByType, String> callerIdMapper ) {
            this.windowMapper = windowMapper;
            this.callerIdMapper = callerIdMapper;
        }

        @Override
        public String windowType() {
            return name();
        }

        @Override
        public RequestsPerWindowSecs extractRequestsPerWindowFrom( LimiterMapping properties ) {
            return windowMapper.apply( properties );
        }

        @Override
        public String extractCallerIdFrom( CallerIdSupplierByType callerIdSupplier ) {
            return StringUtils.normalizeToNull( callerIdMapper.apply( callerIdSupplier ) );
        }

        public static void addBestTo( Map<CompoundKey, InternalLimiterFactory> map,
                                      LimiterMapping properties, CallerIdSupplierByType callerIdSupplier ) {
            if ( properties != null ) {
                for ( NON_GLOBAL value : values() ) {
                    if ( value.addTo( map, properties, callerIdSupplier ) ) {
                        return;
                    }
                }
            }
        }
    }

    WindowType[] ALL_WINDOW_TYPES = {NON_GLOBAL.CredentialsID, NON_GLOBAL.RemoteAddressID, NON_GLOBAL.NoID, GLOBAL};
}

