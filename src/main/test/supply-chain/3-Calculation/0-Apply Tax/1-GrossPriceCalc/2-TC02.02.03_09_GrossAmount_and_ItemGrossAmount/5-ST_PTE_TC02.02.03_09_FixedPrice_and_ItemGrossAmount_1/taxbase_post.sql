update CBD_TAX_RATE_T set TAX_RATE=7 where GA_CODE='DE' and TAX_CODE='1' and VALID_FROM=TO_DATE('01-JAN-00', 'DD-MON-RR') and TAX_RATE=3;
delete from CBD_TAX_RATE_T where SEQ_NO_TR between 5000000 and 6000000;
