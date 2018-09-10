package ru.bpc.cm.utils;

import org.springframework.util.StringUtils;
import ru.bpc.cm.management.Institute;
import ru.bpc.structs.collection.SeparatePrintedList;

import java.util.*;

public class CmUtils {


	private static int getHrsBetweenTwoDates(Calendar start,Calendar finish){
		return getDaysBetweenTwoDates(start, finish)*24-start.get(Calendar.HOUR_OF_DAY)+finish.get(Calendar.HOUR_OF_DAY);
	}
	
	private static int getHrsBetweenTwoDates(Date startDate,Date finishDate){
		Calendar start = Calendar.getInstance();
		Calendar finish = Calendar.getInstance();
		start.setTime(startDate);
		finish.setTime(finishDate);
		return getDaysBetweenTwoDates(start, finish)*24-start.get(Calendar.HOUR_OF_DAY)+finish.get(Calendar.HOUR_OF_DAY);
	}

	
	public static int getDaysBetweenTwoDates(Calendar start,Calendar finish){
		int yearsDiff = finish.get(Calendar.YEAR) - start.get(Calendar.YEAR);
		if(yearsDiff==0){
			return finish.get(Calendar.DAY_OF_YEAR) - start.get(Calendar.DAY_OF_YEAR);
		} else {
			int daysBetween = finish.get(Calendar.DAY_OF_YEAR);
			daysBetween +=start.getActualMaximum(Calendar.DAY_OF_YEAR) - start.get(Calendar.DAY_OF_YEAR);
			for(int i =1; i<yearsDiff; i++){
				start.add(Calendar.YEAR, 1);
				daysBetween +=start.getActualMaximum(Calendar.DAY_OF_YEAR);
			}
			return daysBetween;
		}
	}

	public static Date truncateDate(Date srcDate){
		Calendar cal = Calendar.getInstance(); // locale-specific
		cal.setTime(srcDate);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	public static <T> T getNVLValue(T obj,T defValue){
		return obj == null ? defValue : obj;
	}
	
	public static <T extends Comparable<T>> T getMinValue(T obj1,T obj2){
		return obj1.compareTo(obj2) <= 0 ? obj1 : obj2;
	}
	
	public static <T extends Comparable<T>> T getMaxValue(T obj1,T obj2){
		return obj1.compareTo(obj2) >= 0 ? obj1 : obj2;
	}

	public static <T extends Enum<T> & IIdentifiable> T getEnumValueById(Class<T> enumClass, int id) {
		T result = null;
		for (Enum<T> element : enumClass.getEnumConstants()) {
			@SuppressWarnings("unchecked")
			T value = (T) element;
			if (value.getId() == id) {
				result = value;
				break;
			}
		}
		return result;
	}
}
