#!/bin/bash
/**********************************/
/*       ALTER METRICES TABLE     */
/**********************************/
ALTER TABLE metrices
ALTER COLUMN type TYPE VARCHAR USING 
CASE WHEN type= 0 THEN 'FORBIDDEN_REQUEST'
     WHEN type= 1 THEN 'AUTHORIZED_REQUEST'
      WHEN type= 2 THEN 'RESPONSE'
      WHEN type= 3 THEN 'EMPTY_RESPONSE'
     ELSE 'UNKNOWN'
END;

/**********************************/
/*   ALTER SERVICE APIS TABLE     */
/**********************************/
ALTER TABLE service_apis
ALTER COLUMN service_access_payment_policy TYPE VARCHAR USING 
CASE WHEN service_access_payment_policy= 0 THEN 'MONTHLY_FEE'
     WHEN service_access_payment_policy= 1 THEN 'WELL_DEFINED_PRICE'
      WHEN service_access_payment_policy= 2 THEN 'FOR_FREE'
     ELSE 'UNKNOWN'
END;


ALTER TABLE service_apis
ALTER COLUMN service_access_permission_policy TYPE VARCHAR USING 
CASE WHEN service_access_permission_policy= 0 THEN 'PERMISSION_NECESSARY'
     WHEN service_access_permission_policy= 1 THEN 'PUBLIC'
     ELSE 'UNKNOWN'
END;
