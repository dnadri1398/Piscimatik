package proyecto.Clases;

import java.util.Calendar;
import java.util.Date;

public class luces_estado {
	private int id;
	private int idDepuradora;
	private int encendido;
	private Long fecha;
	
	public luces_estado(int id, int idDepuradora, int encendido, Long fecha) {		
		this.id = id;
		this.idDepuradora = idDepuradora;
		this.encendido = encendido;
		this.fecha = fecha;
	}
	public luces_estado(int id, int idDepuradora, int encendido) {		
		this.id = id;
		this.idDepuradora = idDepuradora;
		this.encendido = encendido;
	}
	public luces_estado(int id, int idDepuradora) {		
		this.id = id;
		this.idDepuradora = idDepuradora;
	}
	
	//ToString
	public String toString() {
		return "[id=" + id + ", idDepuradora=" + idDepuradora + ", encendido=" + encendido + ", fecha="
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
	
	public int getidDepuradora() {
		return idDepuradora;
	}
	public void setidDepuradora(int idDepuradora) {
		this.idDepuradora = idDepuradora;
	}

	public float getEncendido() {
		return encendido;
	}
	public void setEncendido(int encendido) {
		this.encendido = encendido;
	}
	public Long getFecha() {
		return fecha;
	}
	public void setFecha(Long fecha) {
		this.fecha = fecha;
	}
	
}
