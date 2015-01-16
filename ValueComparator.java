

import java.util.Comparator;
import java.util.Map;

class ValueComparator implements Comparator<Integer>
{
    Map<Integer, Integer> base;

    public ValueComparator(Map<Integer, Integer> base){
        this.base = base;
    }

    /*
    public int compare(Object a , Object b)
    {
        if ((Integer) base.get(a) < (Integer) base.get(b)){
            return 1;
        }else if (base.get(a) == base.get(b)){
            return 0;
        }else{
            return - 1;
        }
    }
    */
    
    public int compare(Integer item1, Integer item2){
		// compare the frequency
		int compare = base.get(item2) - base.get(item1);
		// if the same frequency, we check the lexical ordering!
		if(compare == 0){ 
			return (item1 - item2);
		}
		// otherwise, just use the frequency
		return compare;
	}
}


