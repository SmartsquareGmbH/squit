update cbd_exchange_rate_t set DELETE_DATE=null where CUR_CODE_FROM='EUR' and CUR_CODE_TO='CNY' and PERIOD='2014' and VALID_FROM=TO_DATE('06-FEB-13', 'DD-MON-RR');
