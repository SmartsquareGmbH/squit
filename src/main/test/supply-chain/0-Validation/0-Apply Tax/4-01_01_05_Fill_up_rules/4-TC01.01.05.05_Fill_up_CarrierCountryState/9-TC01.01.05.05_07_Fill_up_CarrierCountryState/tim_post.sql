update CBD_BU_MAIN_ADDRESS_T set STA_CODE='CAON'  where bu_code='3101' and bu_type='COM' and valid_from=to_date('22-JUN-10','DD-MON-YY');
delete from IC_UNSPSC_T where  SEQ_NO_UNSP between 5000000 and 6000000;
delete from IC_ITEM_CLASS_T where  SEQ_NO_IC between 5000000 and 6000000;
