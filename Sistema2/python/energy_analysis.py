import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import os
from itertools import combinations
from tqdm import tqdm
from pathlib import Path

# --- Constantes de la Simulación ---
G = 1.0   # Constante de gravitación
H = 0.05  # Parámetro de suavizado
MASS = 1.0 # Masa unitaria de cada partícula

def parse_simulation_output(filename):
    """
    Procesa el archivo de salida CSV con el nuevo formato (t=...).
    Retorna un diccionario que mapea el tiempo (float) a un DataFrame de partículas.
    """
    timesteps = {}
    current_time = None
    particle_data = []

    print(f"Leyendo el archivo de simulación: {filename}...")

    with open(filename, 'r') as f:
        lines = f.readlines()

    with tqdm(total=len(lines), desc="Procesando líneas") as pbar:
        for line in lines:
            line = line.strip()
            if not line:
                pbar.update(1)
                continue

            # --- CAMBIO CLAVE: Detectar header de tiempo ---
            if line.startswith('t='):
                if current_time is not None and particle_data:
                    # El nuevo formato tiene 8 columnas: id, galaxyId, x, y, z, vx, vy, vz
                    df = pd.DataFrame(particle_data,
                                      columns=['id', 'galaxyId', 'x', 'y', 'z', 'vx', 'vy', 'vz'])
                    df = df.apply(pd.to_numeric)
                    timesteps[current_time] = df

                try:
                    current_time = float(line.split('=')[1])
                    particle_data = []
                except (ValueError, IndexError):
                    continue
            # --- CAMBIO CLAVE: Leer línea de partícula ---
            else:
                parts = line.split(';')
                if len(parts) >= 8: # Asegurarse de que la línea tiene suficientes datos
                    if current_time is not None:
                        particle_data.append(parts[:8])
            pbar.update(1)

    # Guarda el último bloque de datos
    if current_time is not None and particle_data:
        df = pd.DataFrame(particle_data,
                          columns=['id', 'galaxyId', 'x', 'y', 'z', 'vx', 'vy', 'vz'])
        df = df.apply(pd.to_numeric)
        timesteps[current_time] = df

    print(f"Se procesaron {len(timesteps)} pasos de tiempo.")
    return timesteps

def calculate_energies(particles_df):
    """
    Calcula Energía Cinética, Potencial y Total.
    (Esta función no necesita cambios)
    """
    # Energía Cinética
    velocities_sq = particles_df[['vx', 'vy', 'vz']]**2
    kinetic_energies = 0.5 * MASS * velocities_sq.sum(axis=1)
    total_ke = kinetic_energies.sum()

    # Energía Potencial
    total_pe = 0
    positions = particles_df[['x', 'y', 'z']].values
    for i, j in combinations(range(len(positions)), 2):
        pos_i = positions[i]
        pos_j = positions[j]
        dist_sq = np.sum((pos_i - pos_j)**2)
        pe_pair = -G * MASS * MASS / np.sqrt(dist_sq + H**2)
        total_pe += pe_pair

    total_energy = total_ke + total_pe
    return total_ke, total_pe, total_energy

def main():
    # --- CAMBIO CLAVE: Usar Pathlib para una ruta más robusta ---
    # Asegúrate de que el nombre del archivo sea el correcto
    data_file_path = Path(__file__).parent / "data" / "sim_dt_0.001.csv"

    try:
        print(f"Buscando archivo de datos en: {data_file_path.resolve()}")
        data_by_time = parse_simulation_output(data_file_path)
    except FileNotFoundError:
        print(f"Error: No se encontró el archivo en '{data_file_path}'.")
        return

    if not data_by_time:
        print("No se procesaron datos. El archivo podría estar vacío o mal formateado.")
        return

    # Calcular energías
    times = sorted(data_by_time.keys())
    kinetic_energies = []
    potential_energies = []
    total_energies = []

    print("\nCalculando energías para cada paso de tiempo...")
    for t in tqdm(times, desc="Calculando Energías"):
        particles = data_by_time[t]
        ke, pe, te = calculate_energies(particles)
        kinetic_energies.append(ke)
        potential_energies.append(pe)
        total_energies.append(te)

    # Graficar
    plt.figure(figsize=(12, 8))
    plt.plot(times, total_energies, marker='o', linestyle='-', markersize=3, label='Energía Total')
    plt.plot(times, kinetic_energies, linestyle='--', label='Energía Cinética')
    plt.plot(times, potential_energies, linestyle='--', label='Energía Potencial')

    plt.xlabel('Tiempo de Simulación (s)')
    plt.ylabel('Energía')
    plt.title('Conservación de la Energía en la Simulación')
    plt.grid(True)

    if total_energies:
        initial_energy = total_energies[0]
        plt.axhline(y=initial_energy, color='r', linestyle='--',
                    label=f'Energía Inicial ({initial_energy:.2f})')

    plt.legend()
    plt.tight_layout()

    output_filename = data_file_path.parent / "energy_components_plot.png"
    plt.savefig(output_filename)
    print(f"\n¡Análisis completo! Gráfico guardado en: {output_filename}")

if __name__ == "__main__":
    main()