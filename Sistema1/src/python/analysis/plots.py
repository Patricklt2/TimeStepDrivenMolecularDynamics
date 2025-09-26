import matplotlib.pyplot as plt

def plot_trajectories(analytical_data, sim_data_dict, output_filename="trajectory_comparison.png"):
    plt.figure(figsize=(12, 8))
    
    plt.plot(analytical_data['Time'], analytical_data['Position'], 'k--', label='Solución Analítica', linewidth=2)
    
    for name, df in sim_data_dict.items():
        plt.plot(df['t'], df['pos'], label=name, alpha=0.8)
        
    plt.title('Comparación de Soluciones Numéricas vs. Analítica')
    plt.xlabel('Tiempo (s)')
    plt.ylabel('Posición (m)')
    plt.legend()
    plt.grid(True)
    plt.savefig(output_filename)

def plot_error_vs_dt(error_data, output_filename="error_vs_dt.png"):
    plt.figure(figsize=(12, 8))
    
    for name, data in error_data.items():
        dts = [item[0] for item in data]
        mses = [item[1] for item in data]
        plt.plot(dts, mses, 'o-', label=name)
        
    plt.title('Error Cuadrático Medio vs. Paso Temporal (dt)')
    plt.xlabel('Paso Temporal dt (s)')
    plt.ylabel('Error Cuadrático Medio (m²)')
    plt.xscale('log')
    plt.yscale('log')
    plt.legend()
    plt.grid(True, which="both", ls="--")
    plt.savefig(output_filename)