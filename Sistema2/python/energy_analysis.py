import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import os
from itertools import combinations

# --- Constantes de la Simulación ---
G = 1.0   # Constante de gravitación
H = 0.05  # Parámetro de suavizado
MASS = 1.0 # Masa unitaria de cada partícula

def parse_simulation_output(filename="sim.csv"):
    """
    Procesa el archivo de salida CSV de la simulación.
    Retorna un diccionario que mapea el tiempo (float) a un DataFrame de partículas.
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

            # Encabezado de galaxia
            if len(parts) == 5 and 'Galaxy' in parts[1]:
                try:
                    time_val = float(parts[0])
                    if current_time is not None and particle_data:
                        df = pd.DataFrame(particle_data,
                                          columns=['id', 'x', 'y', 'z', 'vx', 'vy', 'vz', 'fx', 'fy', 'fz'])
                        df = df.apply(pd.to_numeric)
                        timesteps[current_time] = df
                    current_time = time_val
                    particle_data = []
                except (ValueError, IndexError):
                    continue
            # Línea de partícula
            elif len(parts) == 10:
                if current_time is not None:
                    particle_data.append(parts)

    if current_time is not None and particle_data:
        df = pd.DataFrame(particle_data,
                          columns=['id', 'x', 'y', 'z', 'vx', 'vy', 'vz', 'fx', 'fy', 'fz'])
        df = df.apply(pd.to_numeric)
        timesteps[current_time] = df

    print(f"Se procesaron {len(timesteps)} pasos de tiempo.")
    return timesteps

def calculate_energies(particles_df):
    """
    Calcula Energía Cinética, Potencial y Total.
    Retorna: (KE, PE, Total)
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
    # Construcción de la ruta
    try:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        data_file_path = os.path.join(script_dir, "data", "sim.csv")
    except NameError:
        data_file_path = "python/data/sim.csv"

    # Procesar archivo
    try:
        print(f"Buscando archivo de datos en: {os.path.abspath(data_file_path)}")
        data_by_time = parse_simulation_output(data_file_path)
    except FileNotFoundError:
        print(f"Error: No se encontró el archivo en '{data_file_path}'.")
        return

    if not data_by_time:
        print("No se procesaron datos. El archivo 'sim.csv' podría estar vacío o mal formateado.")
        return

    # Calcular energías
    times = sorted(data_by_time.keys())
    kinetic_energies = []
    potential_energies = []
    total_energies = []

    for t in times:
        particles = data_by_time[t]
        ke, pe, te = calculate_energies(particles)
        kinetic_energies.append(ke)
        potential_energies.append(pe)
        total_energies.append(te)
        print(f"Tiempo: {t:.2f}, KE={ke:.4f}, PE={pe:.4f}, Total={te:.4f}")

    # Graficar
    plt.figure(figsize=(12, 8))
    plt.plot(times, total_energies, marker='o', linestyle='-', markersize=3, label='Energía Total')
    plt.plot(times, kinetic_energies, linestyle='--', label='Energía Cinética')
    plt.plot(times, potential_energies, linestyle='--', label='Energía Potencial')

    plt.xlabel('Tiempo de Simulación (s)')
    plt.ylabel('Energía')
    plt.title('Energía Cinética, Potencial y Total en la Simulación')
    plt.grid(True)

    if total_energies:
        initial_energy = total_energies[0]
        plt.axhline(y=initial_energy, color='r', linestyle='--',
                    label=f'Energía Inicial ({initial_energy:.2f})')

    plt.legend()
    plt.tight_layout()

    output_filename = os.path.join(os.path.dirname(data_file_path), "energy_components_plot.png")
    plt.savefig(output_filename)
    print(f"\n¡Análisis completo! Gráfico guardado en: {output_filename}")

if __name__ == "__main__":
    main()
