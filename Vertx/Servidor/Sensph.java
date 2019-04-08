package proyecto.Clases;

import java.util.Calendar;
import java.util.Date;

public class Sensph {
	private int idsensph;
	private int idDepuradora;
	private float valor;
	private Long fecha;
	
	public Sensph(int idsensph, int idDepuradora, float valor, Long fecha) {		
		this.idsensph = idsensph;
		this.idDepuradora = idDepuradora;
		this.valor = valor;
		this.fecha = fecha;
	}
	public Sensph(int idsensph, int idDepuradora, float valor) {		
		this.idsensph = idsensph;
		this.idDepuradora = idDepuradora;
		this.valor = valor;
	}
	public Sensph(int idsensph, int idDepuradora) {		
		this.idsensph = idsensph;
		this.idDepuradora = idDepuradora;
	}
	
	
	//ToString
	public String toString() {
		return "[idsensph=" + idsensph + ", idDepuradora=" + idDepuradora + ", valor=" + valor + ", fecha="
				+ fecha + "]";
	}
	
	public Date getFechaDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(fecha);
		return calendar.getTime();
	}
	
	//getter y setters
	public int getIdsensph() {
		return idsensph;
	}
	public void setIdsensph(int idsensph) {
		this.idsensph = idsensph;
	}
	public int getIdDepuradora() {
		return idDepuradora;
	}
	public void setIdDepuradora(int idDepuradora) {
		this.idDepuradora = idDepuradora;
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
