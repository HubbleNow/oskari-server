package flyway.peltodata;

import fi.nls.oskari.db.BundleHelper;
import fi.nls.oskari.domain.map.view.Bundle;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;

import java.sql.Connection;

public class V1_1_3__AddPeltodataBundle implements JdbcMigration {
    private static final String BUNDLE_ID = "peltodata";

    @Override
    public void migrate(Connection connection) throws Exception {
            // BundleHelper checks if these bundles are already registered
            Bundle bundle = new Bundle();
            bundle.setName(BUNDLE_ID);
            BundleHelper.registerBundle(bundle, connection);
    }
}


