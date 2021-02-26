import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main {
		
	public static void main(String[] args) {
		File file = new File("C:\\Users\\Andrey\\Desktop\\pdf1.jpg");
		File file1 = new File("C:\\Users\\Andrey\\Desktop\\pdf2.jpg");
		FileInputStream input = null;
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file1);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			input = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
        byte[] buffer = null;
        int bit = 7;
        try {
			int size = input.available();
			int check = 0;
			System.out.println(size);
			while(size > bit) {
				size -= bit;
				check++;
			}
			boolean control = false;
			int dop = 0;
			if(size != 0) {
				control = true;
				dop = size;
			}
			buffer = new byte[bit];
			System.out.println(check + " + " + dop);
			for(int i = 0; i < check; i++) {
				input.read(buffer, 0, buffer.length);
				for(int g = 0; g < bit; g++) {
					buffer[g] += 1;
				}
				out.write(buffer, 0, buffer.length);
			}
			if(control) {
				buffer = new byte[dop];
				input.read(buffer, 0, buffer.length);
				out.write(buffer, 0, buffer.length);
	            input.close();
	            for(int g = 0; g < dop; g++) {
					buffer[g] += 1; 
				}
			}
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}
}