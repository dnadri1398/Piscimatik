package dad.us.Poolmatik;

public class Depuradora {
	
	private int idDepuradora;
	private Long fechaAlta;
	private String Usuario;
	private String Direccion;
	private int Telefono;
	private String Ciudad;
	private String Modelo;
	private int Caudal;
	private int Potencia;
	private int Volumen;
	
	public Depuradora(int idDepuradora, Long fechaAlta, String usuario, String direccion, int telefono, String ciudad,
			String modelo, int caudal, int potencia, int volumen) {
		super();
		this.idDepuradora = idDepuradora;
		this.fechaAlta = fechaAlta;
		Usuario = usuario;
		Direccion = direccion;
		Telefono = telefono;
		Ciudad = ciudad;
		Modelo = modelo;
		Caudal = caudal;
		Potencia = potencia;
		Volumen = volumen;
	}
	
	public Depuradora() {
		this(0,new Long(System.currentTimeMillis()),"","",0,"","",0,0,0);
	}

	public int getIdDepuradora() {
		return idDepuradora;
	}

	public void setIdDepuradora(int idDepuradora) {
		this.idDepuradora = idDepuradora;
	}

	public Long getFechaAlta() {
		return fechaAlta;
	}

	public void Long(Long fechaAlta) {
		this.fechaAlta = fechaAlta;
	}

	public String getUsuario() {
		return Usuario;
	}

	public void setUsuario(String usuario) {
		Usuario = usuario;
	}

	public String getDireccion() {
		return Direccion;
	}

	public void setDireccion(String direccion) {
		Direccion = direccion;
	}

	public int getTelefono() {
		return Telefono;
	}

	public void setTelefono(int telefono) {
		Telefono = telefono;
	}

	public String getCiudad() {
		return Ciudad;
	}

	public void setCiudad(String ciudad) {
		Ciudad = ciudad;
	}

	public String getModelo() {
		return Modelo;
	}

	public void setModelo(String modelo) {
		Modelo = modelo;
	}

	public int getCaudal() {
		return Caudal;
	}

	public void setCaudal(int caudal) {
		Caudal = caudal;
	}

	public int getPotencia() {
		return Potencia;
	}

	public void setPotencia(int potencia) {
		Potencia = potencia;
	}

	public int getVolumen() {
		return Volumen;
	}

	public void setVolumen(int volumen) {
		Volumen = volumen;
	}
	
	
	
}
