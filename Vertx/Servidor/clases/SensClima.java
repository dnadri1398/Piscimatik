package proyecto.Clases;

public class SensClima {
	private int id;
	private int idSen;
	private int valorTemp;
	private int valorHumedad;
	private long fecha;
	public SensClima(int id, int idSen, int valorTemp, int valorHumedad, long fecha) {
		super();
		this.id = id;
		this.idSen = idSen;
		this.valorTemp = valorTemp;
		this.valorHumedad = valorHumedad;
		this.fecha = fecha;
	}
	public SensClima(int id, int idSen, int valorTemp, int valorHumedad) {
		super();
		this.id = id;
		this.idSen = idSen;
		this.valorTemp = valorTemp;
		this.valorHumedad = valorHumedad;
	}
	public SensClima(int id, int idSen, int valorTemp) {
		super();
		this.id = id;
		this.idSen = idSen;
		this.valorTemp = valorTemp;
	}
	public SensClima(int id, int idSen) {
		super();
		this.id = id;
		this.idSen = idSen;
	}
	
		
	@Override
	public String toString() {
		return "[id=" + id + ", idSen=" + idSen + ", valorTemp=" + valorTemp
				+ ", valorHumedad=" + valorHumedad + ", fecha=" + fecha + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + idSen;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SensClima other = (SensClima) obj;
		if (id != other.id)
			return false;
		if (idSen != other.idSen)
			return false;
		return true;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public int getIdSen() {
		return idSen;
	}
	public void setIdSen(int idSen) {
		this.idSen = idSen;
	}
	public int getValorTemp() {
		return valorTemp;
	}
	public void setValorTemp(int valorTemp) {
		this.valorTemp = valorTemp;
	}
	public int getValorHumedad() {
		return valorHumedad;
	}
	public void setValorHumedad(int valorHumedad) {
		this.valorHumedad = valorHumedad;
	}
	public long getFecha() {
		return fecha;
	}
	public void setFecha(long fecha) {
		this.fecha = fecha;
	}
	
	
	
}
