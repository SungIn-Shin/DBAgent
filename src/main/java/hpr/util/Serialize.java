package hpr.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Serialize {

	ByteArrayOutputStream bos_ = null;
	ObjectOutputStream out_ = null;
	
	ByteArrayInputStream bis_ = null;
	ObjectInputStream in_ = null;

	public byte[] marshalling (Object vo) throws IOException {
		close();
		
		bos_ = new ByteArrayOutputStream();
		out_ = new ObjectOutputStream(bos_);

		out_.writeObject(vo);
		
		return bos_.toByteArray();
	}

	public Object unmarshalling( byte[] value ) throws IOException, ClassNotFoundException {
		close();
		
		bis_ = new ByteArrayInputStream( value );
		in_ = new ObjectInputStream(bis_);

		return (Object)in_.readObject();
	}
	
	public void close() {
		try {
			if( null != out_ )
				out_.close();
		} catch( IOException ex ) {}
		out_ = null;
		
		try {
			if( null != bos_)
				bos_.close();
		} catch( IOException ex ) {}
		bos_ = null;

		try {
			if( null != in_ )
				in_.close();
		} catch( IOException ex ) {}
		in_ = null;

		try {
			if( null != bis_)
				bis_.close();
		} catch( IOException ex ) {}
		bis_ = null;
	}

}
