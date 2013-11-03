
public class KVEntry<T> {

	private int key;
	private T value;
	
	
	public KVEntry(int key, T value) {
		this.key = key;
		this.value = value;
	}
	
	public int getKey() {
		return key;
	}
	
	public T getValue() {
		return value;
	}
	
	public void setValue(T value) {
		this.value = value;
	}
	
}
