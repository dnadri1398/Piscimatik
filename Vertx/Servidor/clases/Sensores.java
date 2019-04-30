package proyecto.Clases;


//Se usará esta clase para manejar las consultas con una sola funcion,
//en esta clase entran sensph,senscloro, senstemp, sensproxphup, sensproxphdown, sensproxcloro
//de momento puede ser sustitutivo del resto de clases de sensores
public class Sensores {

	private int id;
	private int idDepuradora;
	private String nombre;
	
	public Sensores(int id,int idDepuradora,String nombre) {
		this.id = id;
		this.idDepuradora = idDepuradora;
		this.nombre = nombre;
	}
	public Sensores(int id,int idDepuradora) {
		this.id = id;
		this.idDepuradora = idDepuradora;
	}
	
	
	
	
	
	@Override
	public String toString() {
		return "[id= " + id + ", idDepuradora= " + idDepuradora + ", nombre= " + nombre + "]";
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + idDepuradora;
		result = prime * result + id;
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
		if (id != other.id)
			return false;
		return true;
	}
	public int getId() {
		return id;
	}
	public int getIdDepuradora() {
		return idDepuradora;
	}
	public void setIdDepuradora(int idDepuradora) {
		this.idDepuradora = idDepuradora;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public String getNombre() {
		return nombre;
	}
	
	
	
}
