package ru.bpc.cm.utils;

import java.util.ArrayList;
import java.util.List;

public class CollectionUtils {
	public static <T> List<List<T>> splitListBySizeView(List<T> list, final int L){
		List<List<T>> parts = null;
		if (L!=0){
			parts = new ArrayList<List<T>>();
		    final int N = list.size();
		    for (int i = 0; i < N; i += L) {
		        parts.add(list.subList(i, Math.min(N, i + L)));
		    }
		}
		
	    return parts;
	}
}
