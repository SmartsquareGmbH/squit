delete from CBD_TAX_REPRESENTATIVE_T where SEQ_NO_TREP between 50000000 and 60000000;
delete from CBD_TAX_REG_NUMBERS_T where SEQ_NO_TREG between 5000000 and 6000000;
update CBD_TAX_REG_NUMBERS_T SET VALID_TO =null where TREG_NO='NL004445879B01' and BU_CODE='1110' and TREG_TYPE='DV' and BU_TYPE='COM';