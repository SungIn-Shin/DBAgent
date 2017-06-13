package com.hopper.dbagent.excel;

import com.hopper.dbagent.StackTrace;
import com.hopper.dbagent.Utils;
import com.hopper.dbagent.Validator;
import com.hopper.dbagent.vo.MessageTempVO;
import com.monitorjbl.xlsx.StreamingReader;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import hpr.util.Pair;

public class ExcelHandler {

	private static final Logger log = LoggerFactory.getLogger(ExcelHandler.class);
	private String excelFile;
	private Workbook workbook;
	private FileInputStream fis = null;
	private String jobKey, jobName, msgType, callback, resvYN;
	private Date sendDate;

	public static final int CELL_COUNT = 6;

	public ExcelHandler( String excelFile , String jobKey, String jobName, String msgType, String callback, String resvYN, Date sendDate ) {
		this.excelFile 	= excelFile;
		this.jobKey 		= jobKey;
		this.jobName 		= jobName;
		this.msgType 		= msgType;
		this.callback 	= callback.replaceAll("[^0-9]", "");
		this.resvYN 		= resvYN;
		this.sendDate 	= sendDate;
		init();
	}

	private void init() {
		// 파일 확장자 추출
		String fileNameExtension = excelFile.substring(excelFile.lastIndexOf("."));

		try {
			fis = new FileInputStream(new File(excelFile));
			if( ".xls".equals(fileNameExtension) ) {
				workbook = new HSSFWorkbook(fis);
				log.info("{} File handling", excelFile);
			}
			else if( ".xlsx".equals(fileNameExtension) ) {
				workbook = StreamingReader.builder().rowCacheSize(100).bufferSize(4096).open(fis);
//				 workbook = new XSSFWorkbook(fis);
				log.info("{} File handling", excelFile);
			}
			else {
				log.warn("Excel 파일만 주세요.");
			}
		}
		catch( Exception e ) {
			log.info(StackTrace.toString(e));
		}

	}

	public Pair<List<Object>, List<Object>> readExcelData() {
		Pair<List<Object>, List<Object>> result = null;

		if( workbook instanceof HSSFWorkbook ) {
			result = readXLSExcelData();
		}
		else {
			result = readXLSXExcelData();
//			result = readXLSExcelData();
		}
		return result;
	}


	private Pair<List<Object>, List<Object>> readXLSExcelData() {

		List<Object> succList = new ArrayList<>();
		List<Object> failList = new ArrayList<>();
		// Cell 의 갯수
		try{
			Iterator<Sheet> sheetIter = workbook.sheetIterator();
			while( sheetIter.hasNext() ) {
				Sheet sheet = sheetIter.next();
				int rows = sheet.getPhysicalNumberOfRows();
				for( int rowIdx = 4; rowIdx <= rows; rowIdx++ ) {
					Row row = sheet.getRow( rowIdx );
					if(row != null) {
						MessageTempVO excelDataVO = new MessageTempVO(jobKey, jobName, msgType, callback, resvYN, sendDate);
						for (int cellIdx = 0; cellIdx <= CELL_COUNT; cellIdx++) {
							Cell cell = row.getCell(cellIdx);
							if (cell != null) {
								setXLSExcelData(excelDataVO, cell);
							}
						}
						Validator.msgTempValidationCheck(excelDataVO);
						if (excelDataVO.sendFlag == "Y") {
							succList.add(excelDataVO);
						}
						else {
							failList.add(excelDataVO);
						}
					}
				}
			}
		}
		catch( Exception e ){
			log.info(StackTrace.toString(e));
		}
		finally {
			try {
				if( fis != null ) fis.close();
				if( workbook != null ) workbook.close();
			}
			catch( IOException e ) {
				log.info(StackTrace.toString(e));
			}
			catch (Exception e) {
				log.info(StackTrace.toString(e));
			}
		}
		log.info("List Succ Size : " + succList.size());
		log.info("List Fail Size : " + failList.size());

		return new Pair<List<Object>, List<Object>>(succList, failList);
	}

	private hpr.util.Pair<List<Object>, List<Object>> readXLSXExcelData() {
		List<Object> succList = new ArrayList<>();
		List<Object> failList = new ArrayList<>();

		try {
			for (Sheet sheet : workbook) {
				System.out.println(sheet.getSheetName());
				for (Row r : sheet) {
					MessageTempVO excelDataVO = new MessageTempVO(jobKey, jobName, msgType, callback, resvYN, sendDate);
					if(r.getRowNum() > 3) {
						for (Cell c : r) {
							setXLSXExcelData(excelDataVO, c);
						}
						Validator.msgTempValidationCheck(excelDataVO);
						if (excelDataVO.sendFlag == "Y") {
							succList.add(excelDataVO);
						}
						else {
							failList.add(excelDataVO);
						}
					}
				}
			}
		}
		catch (Exception e) {
			log.warn(StackTrace.toString(e));
		}
		finally {
			try{
				if ( fis != null ) fis.close();
				if ( workbook != null) workbook.close();
			} catch (Exception e ) {
				log.warn(StackTrace.toString(e));
			}

		}

		return new Pair<List<Object>, List<Object>>(succList, failList);
	}


	private void setXLSXExcelData(MessageTempVO excelData, Cell cell) {
		switch (cell.getColumnIndex()) {
			case 0:
				excelData.tempNo = Long.parseLong(cell.getStringCellValue());
				break;
			case 1:
				excelData.phone = cell.getStringCellValue().replaceAll("[^0-9]", "");
				if ( excelData.phone.isEmpty() ) {
					excelData.phone = "0";
				}
				break;
			case 2:
				excelData.subject = Utils.lengthLimit(cell.getStringCellValue(), 120, null);
				break;
			case 3:
				excelData.text = cell.getStringCellValue();
				break;
			case 4:
				excelData.fileName1 = cell.getStringCellValue();
				break;
			case 5:
				excelData.fileName2 = cell.getStringCellValue();
				break;
			case 6:
				excelData.fileName3 = cell.getStringCellValue();
				break;

			default :
		}
	}

	private void setXLSExcelData(MessageTempVO excelData, Cell cell) {
		//
		switch (cell.getColumnIndex()) {
			case 0:
				excelData.tempNo = Long.parseLong(cellDataToString(cell));
				break;
			case 1:
				excelData.phone = cellDataToString(cell).replaceAll("[^0-9]", "");
				if ( excelData.phone.isEmpty() ) {
					excelData.phone = "0";
				}
				break;
			case 2:
				excelData.subject = Utils.lengthLimit(cellDataToString(cell), 120, null);
				break;
			case 3:
				excelData.text = cellDataToString(cell);
				break;
			case 4:
				excelData.fileName1 = cellDataToString(cell);
				break;
			case 5:
				excelData.fileName2 = cellDataToString(cell);
				break;
			case 6:
				excelData.fileName3 = cellDataToString(cell);
				break;

			default :
		}
	}

	public String cellDataToString(Cell cell) {

		String value = "";
		switch (cell.getCellType()) {
			case Cell.CELL_TYPE_FORMULA:
				value = cell.getCellFormula() + "";
				break;
			case Cell.CELL_TYPE_NUMERIC:
				if(DateUtil.isCellDateFormatted(cell)){
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
					value = sdf.format(cell.getDateCellValue());
				} else {
					value = (int)cell.getNumericCellValue() + "";
				}
				break;
			case Cell.CELL_TYPE_STRING:
				value = cell.getStringCellValue() + "";
				break;
			case Cell.CELL_TYPE_BOOLEAN:
				value = cell.getBooleanCellValue() + "";
				break;
			case Cell.CELL_TYPE_BLANK:
				value = "";
				break;
			case Cell.CELL_TYPE_ERROR:
				value = cell.getErrorCellValue() + "";
				break;
			default:
				value = "DEFAULT";
		}
		return value;
	}




}