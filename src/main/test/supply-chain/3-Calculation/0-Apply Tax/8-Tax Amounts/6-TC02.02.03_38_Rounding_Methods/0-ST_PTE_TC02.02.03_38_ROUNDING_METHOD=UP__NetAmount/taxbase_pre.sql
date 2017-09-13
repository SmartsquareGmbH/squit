update CBD_CURRENCY_T  set  ROUNDING_METHOD='UP', VALID_DECIMALS='2' where CUR_CODE='EUR' and valid_from=to_date('01-JAN-90','DD-MON-RR');
