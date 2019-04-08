package proyecto.Clases;


//Se usará esta clase para manejar las consultas con una sola funcion,
//en esta clase entran sensph,senscloro, senstemp, sensproxphup, sensproxphdown, sensproxcloro
//de momento puede ser sustitutivo del resto de clases de sensores
public class Sensores {

	private int idSensor;
	private int idDepuradora;
	private int valor;
	private long fecha;
	
	public Sensores(int idSensor,int idDepuradora, int valor, long fecha) {
		this.idSensor = idSensor;
		this.idDepuradora = idDepuradora;
		this.valor = valor;
		this.fecha = fecha;
	}
	public Sensores(int idSensor,int idDepuradora, int valor) {
		this.idSensor = idSensor;
		this.idDepuradora = idDepuradora;
		this.valor = valor;
	}
	
	
	
	
	@Override
	public String toString() {
		return "SensTemp [idSensor=" + idSensor + ", idDepuradora=" + idDepuradora + ", valor=" + valor + ", fecha="
				+ fecha + "]";
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + idDepuradora;
		result = prime * result + idSensor;
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
		Sensores other = (Sensores) obj;
		if (idDepuradora != other.idDepuradora)
			return false;
		if (idSensor != other.idSensor)
			return false;
		return true;
	}
	public Sensores(int idDepuradora) {
		this.idDepuradora = idDepuradora;
	}
	public int getIdDepuradora() {
		return idDepuradora;
	}
	public void setIdDepuradora(int idDepuradora) {
		this.idDepuradora = idDepuradora;
	}
	public int getValor() {
		return valor;
	}
	public void setValor(int valor) {
		this.valor = valor;
	}
	public long getFecha() {
		return fecha;
	}
	public void setFecha(long fecha) {
		this.fecha = fecha;
	}
	
	
}
