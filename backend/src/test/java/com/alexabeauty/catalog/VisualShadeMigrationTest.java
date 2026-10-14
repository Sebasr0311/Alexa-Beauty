package com.alexabeauty.catalog;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class VisualShadeMigrationTest {
    @Test void upgradeV4PreservesEveryExistingColumnAndLegacyDuplicate() throws Exception {
        String url=System.getenv("SHADE_MIGRATION_DATABASE_URL");
        if(url==null) url="jdbc:h2:mem:shade_migration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        if(!url.startsWith("jdbc:h2:mem:")&&!url.startsWith("jdbc:postgresql://127.0.0.1:55441/shade_migration"))
            throw new IllegalStateException("Migration rehearsal restricted to isolated local database");
        String user=url.startsWith("jdbc:h2")?"sa":"shade_test";
        String schema="shades_"+UUID.randomUUID().toString().replace("-","");
        Flyway.configure().dataSource(url,user,"").schemas(schema).defaultSchema(schema).target("4").load().migrate();
        try(Connection connection=DriverManager.getConnection(url,user,"")) {
            connection.setSchema(schema);
            String product=UUID.randomUUID().toString(),variant=UUID.randomUUID().toString(),order=UUID.randomUUID().toString();
            try(Statement statement=connection.createStatement()) {
                statement.executeUpdate("INSERT INTO products(id,sku,name,slug,brand_id,category_id) SELECT '"+product+"','LEGACY-P','Legacy base','legacy-base',b.id,c.id FROM brands b CROSS JOIN categories c LIMIT 1");
                statement.executeUpdate("INSERT INTO product_variants(id,product_id,sku,shade_name,presentation,sale_price,stock_on_hand,stock_reserved) VALUES ('"+variant+"','"+product+"','LEGACY-V','Ivory','30 ml',25000,8,2)");
                statement.executeUpdate("INSERT INTO product_variants(id,product_id,sku,shade_name,presentation,sale_price,stock_on_hand) VALUES ('"+UUID.randomUUID()+"','"+product+"','LEGACY-DUP','Ivory','30 ml',26000,3)");
                statement.executeUpdate("INSERT INTO product_images(id,product_id,variant_id,storage_path,is_cover) VALUES ('"+UUID.randomUUID()+"','"+product+"','"+variant+"','legacy/image.jpg',TRUE)");
                statement.executeUpdate("INSERT INTO orders(id,reference,customer_name,customer_phone,delivery_type,subtotal,total,status,expires_at,tracking_token_hash) VALUES ('"+order+"','LEGACY-ORDER','Historical customer','573001234567','DOMICILIO',25000,25000,'PENDIENTE_CONTACTO',CURRENT_TIMESTAMP,'legacy-hash')");
                statement.executeUpdate("INSERT INTO order_items(id,order_id,variant_id,sku,product_name,variant_name,unit_price,quantity,subtotal) VALUES ('"+UUID.randomUUID()+"','"+order+"','"+variant+"','LEGACY-V','Legacy base','Historical Ivory',25000,1,25000)");
                statement.executeUpdate("INSERT INTO stock_reservations(id,order_id,variant_id,quantity,expires_at) VALUES ('"+UUID.randomUUID()+"','"+order+"','"+variant+"',2,CURRENT_TIMESTAMP)");
                statement.executeUpdate("INSERT INTO inventory_movements(id,variant_id,movement_type,quantity_delta,qty_before,qty_after) VALUES ('"+UUID.randomUUID()+"','"+variant+"','ENTRADA_INICIAL',8,0,8)");
                statement.executeUpdate("INSERT INTO payments(id,order_id,payment_method,transfer_reference,amount,status,notes) VALUES ('"+UUID.randomUUID()+"','"+order+"','NEQUI','LEGACY-TRANSFER',25000,'PENDIENTE','Historical payment notes')");
                statement.executeUpdate("INSERT INTO order_status_history(id,order_id,new_status,notes) VALUES ('"+UUID.randomUUID()+"','"+order+"','PENDIENTE_CONTACTO','Historical order status')");
                statement.executeUpdate("INSERT INTO audit_logs(id,action,entity_type,entity_id,before_snapshot,after_snapshot) VALUES ('"+UUID.randomUUID()+"','LEGACY_IMPORT','PRODUCT','"+product+"','unchanged-before','unchanged-after')");
            }
            Map<String,List<String>> columns=new TreeMap<>();
            try(ResultSet tables=connection.getMetaData().getTables(null,schema,"%",new String[]{"TABLE"})) {
                while(tables.next()) {
                    String table=tables.getString("TABLE_NAME");if(table.equals("flyway_schema_history"))continue;
                    var names=new ArrayList<String>();
                    try(ResultSet result=connection.getMetaData().getColumns(null,schema,table,"%")){while(result.next())names.add(result.getString("COLUMN_NAME"));}
                    columns.put(table,names);
                }
            }
            var before=snapshot(connection,columns);
            assertThat(before.get("payments")).hasSize(1);
            assertThat(before.get("order_status_history")).hasSize(1);
            assertThat(before.get("audit_logs")).hasSize(1);
            Flyway.configure().dataSource(url,user,"").schemas(schema).defaultSchema(schema).load().migrate();
            assertThat(snapshot(connection,columns)).isEqualTo(before);
            try(Statement statement=connection.createStatement();ResultSet rows=statement.executeQuery("SELECT COUNT(*) FROM product_variants WHERE shade_code IS NOT NULL OR shade_hex IS NOT NULL")) {
                rows.next();assertThat(rows.getInt(1)).isZero();
            }
            assertThat(columns).containsKeys("orders","order_items","product_images","stock_reservations","inventory_movements");
            System.out.println("V4_TO_V5_PRESERVATION: all "+columns.size()+" old tables and columns equal; payment, order history, audit, image, stock and order fixtures unchanged; new shade fields null.");
        }
    }
    private Map<String,List<String>> snapshot(Connection connection,Map<String,List<String>> columns) throws SQLException {
        Map<String,List<String>> result=new TreeMap<>();
        for(var table:columns.entrySet()) {
            var rows=new ArrayList<String>();
            try(Statement statement=connection.createStatement();ResultSet rs=statement.executeQuery("SELECT "+String.join(",",table.getValue())+" FROM "+table.getKey())) {
                while(rs.next()){var values=new ArrayList<String>();for(int i=1;i<=table.getValue().size();i++)values.add(rs.getString(i));rows.add(values.toString());}
            }
            Collections.sort(rows);result.put(table.getKey(),rows);
        }
        return result;
    }
}
