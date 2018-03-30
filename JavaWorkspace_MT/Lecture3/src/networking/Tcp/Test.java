package networking.Tcp;

import java.io.Serializable;

class Test implements Serializable {
	int key;
	String pass;

	public Test(int key, String pass) {
		super();
		this.key = key;
		this.pass = pass;
	}

	@Override
	public String toString() {
		return "Test [key=" + key + ", pass=" + pass + "]";
	}
}
