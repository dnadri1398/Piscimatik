package proyecto.Clases;

import java.util.Calendar;
import java.util.Date;

public class Sensor {
	private int id;
	private int idsen;
	private float valor;
	private Long fecha;
	
	public Sensor(int id, int idsen, float valor, Long fecha) {		
		this.id = id;
		this.idsen = idsen;
		this.valor = valor;
		this.fecha = fecha;
	}
	public Sensor(int id, int idsen, float valor) {		
		this.id = id;
		this.idsen = idsen;
		this.valor = valor;
	}
	public Sensor(int id, int idsen) {		
		this.id = id;
		this.idsen = idsen;
	}
	
	//ToString
	public String toString() {
		return "[id=" + id + ", idsens=" + idsen + ", valor=" + valor + ", fecha="
				+ fecha + "]";
	}
	
	public Date getFechaDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(fecha);
		return calendar.getTime();
	}
	
	//getter y setters
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public int getIdsen() {
		return idsen;
	}
	public void setIdsen(int idsen) {
		this.idsen = idsen;
	}

	public float getValor() {
		return valor;
	}
	public void setValor(float valor) {
		this.valor = valor;
	}
	public Long getFecha() {
		return fecha;
	}
	public void setFecha(Long fecha) {
		this.fecha = fecha;
	}
	
}
