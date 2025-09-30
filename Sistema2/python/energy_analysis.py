import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import os
from itertools import combinations

# --- Constantes de la Simulación ---
# Asegúrate de que coincidan con tu archivo Simulation.java
G = 1.0  # Constante de gravitación
H = 0.05 # Parámetro de suavizado
MASS = 1.0 # Masa unitaria de cada partícula

def parse_simulation_output(filename="sim.csv"):
    """
    Procesa el archivo de salida CSV de la simulación.

    Este formato es particular, con un encabezado de galaxia por cada
    paso de tiempo, seguido por los datos de cada partícula.

    Retorna:
        Un diccionario que mapea el tiempo (float) a un DataFrame de pandas
        con los datos de las partículas en ese instante.
    """
    timesteps = {}
    current_time = None
    particle_data = []
    
    print(f"Leyendo el archivo de simulación: {filename}...")
    
    with open(filename, 'r') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            
            parts = line.split(';')
            
            # Una línea de encabezado de galaxia tiene 5 partes: tiempo, nombre, x, y, z
            if len(parts) == 5 and 'Galaxy' in parts[1]:
                try:
                    time_val = float(parts[0])
                    # Si ya teníamos datos de partículas, los guardamos antes de empezar un nuevo paso
                    if current_time is not None and particle_data:
                        df = pd.DataFrame(particle_data, columns=['id', 'x', 'y', 'z', 'vx', 'vy', 'vz', 'fx', 'fy', 'fz'])
                        df = df.apply(pd.to_numeric)
                        timesteps[current_time] = df

                    # Empezamos un nuevo paso de tiempo
                    current_time = time_val
                    particle_data = []
                except (ValueError, IndexError):
                    continue # No es una línea de encabezado válida
            # Una línea de partícula tiene 10 partes
            elif len(parts) == 10:
                if current_time is not None:
                    particle_data.append(parts)

    # Guarda el último bloque de datos que quedó en memoria
    if current_time is not None and particle_data:
        df = pd.DataFrame(particle_data, columns=['id', 'x', 'y', 'z', 'vx', 'vy', 'vz', 'fx', 'fy', 'fz'])
        df = df.apply(pd.to_numeric)
        timesteps[current_time] = df
        
    print(f"Se procesaron {len(timesteps)} pasos de tiempo.")
    return timesteps

def calculate_total_energy(particles_df):
    """
    Calcula la energía total (Cinética + Potencial) para un conjunto
    de partículas en un instante de tiempo.
    """
    # --- Cálculo de Energía Cinética ---
    # KE = 0.5 * m * v^2. Como m=1, KE = 0.5 * (vx^2 + vy^2 + vz^2)
    velocities_sq = particles_df[['vx', 'vy', 'vz']]**2
    kinetic_energies = 0.5 * MASS * velocities_sq.sum(axis=1)
    total_ke = kinetic_energies.sum()

    # --- Cálculo de Energía Potencial ---
    # PE = sumatoria sobre pares (i,j) de -G*mi*mj / sqrt(r_ij^2 + h^2)
    # Como m=1, PE = sumatoria de -G / sqrt(r_ij^2 + h^2)
    total_pe = 0
    positions = particles_df[['x', 'y', 'z']].values
    
    # Usamos itertools.combinations para obtener todos los pares únicos de partículas
    for i, j in combinations(range(len(positions)), 2):
        pos_i = positions[i]
        pos_j = positions[j]
        
        dist_sq = np.sum((pos_i - pos_j)**2)
        
        pe_pair = -G * MASS * MASS / np.sqrt(dist_sq + H**2)
        total_pe += pe_pair
        
    return total_ke + total_pe

def main():
    """
    Función principal para ejecutar el análisis.
    """
    # --- CONSTRUCCIÓN DE LA RUTA AL ARCHIVO ---
    # Esto hace que la ruta sea robusta, sin importar desde dónde se llame al script.
    try:
        # Directorio donde se encuentra este script
        script_dir = os.path.dirname(os.path.abspath(__file__))
        # Ruta al archivo de datos
        data_file_path = os.path.join(script_dir, "data", "sim.csv")
    except NameError:
        # Fallback para entornos interactivos (como Jupyter) donde __file__ no existe
        data_file_path = "python/data/sim.csv"


    # 1. Procesar el archivo de datos usando la ruta que construimos
    try:
        print(f"Buscando archivo de datos en: {os.path.abspath(data_file_path)}")
        data_by_time = parse_simulation_output(data_file_path)
    except FileNotFoundError:
        print(f"Error: No se encontró el archivo en '{data_file_path}'.")
        print("Asegúrate de que la simulación en Java se haya ejecutado y guardado el archivo en la carpeta 'python/data/'.")
        return

    # ... (El resto de la función sigue exactamente igual)
    # ... (Cálculo y graficado de la energía)
    
    if not data_by_time:
        print("No se procesaron datos. El archivo 'sim.csv' podría estar vacío o en un formato incorrecto.")
        return

    # 2. Calcular la energía para cada paso de tiempo
    times = sorted(data_by_time.keys())
    total_energies = []
    for t in times:
        particles = data_by_time[t]
        energy = calculate_total_energy(particles)
        total_energies.append(energy)
        print(f"Tiempo: {t:.2f}, Energía Total: {energy:.4f}")

    # 3. Graficar los resultados
    plt.figure(figsize=(12, 8))
    plt.plot(times, total_energies, marker='o', linestyle='-', markersize=3, label='Energía Total')
    plt.xlabel('Tiempo de Simulación (s)')
    plt.ylabel('Energía Total del Sistema')
    plt.title('Conservación de la Energía en la Simulación de Galaxias')
    plt.grid(True)
    
    if total_energies:
        initial_energy = total_energies[0]
        plt.axhline(y=initial_energy, color='r', linestyle='--', label=f'Energía Inicial ({initial_energy:.2f})')
    
    plt.legend()
    plt.tight_layout()

    # Guardar el gráfico en la carpeta python
    output_filename = os.path.join(os.path.dirname(data_file_path), "total_energy_plot.png")
    plt.savefig(output_filename)
    print(f"\n¡Análisis completo! Gráfico guardado en: {output_filename}")

if __name__ == "__main__":
    main()