import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import os
from scipy.spatial.distance import pdist, squareform

# --- Constantes de la Simulación ---
# Asegúrate de que coincidan con tu archivo Simulation.java
G = 1.0  # Constante de gravitación
H = 0.05 # Parámetro de suavizado
MASS = 1.0 # Masa unitaria de cada partícula

def parse_simulation_output(filename="sim.csv"):
    """
    Procesa el archivo de salida CSV de la simulación de forma optimizada.

    Este formato es particular, con un encabezado de galaxia por cada
    paso de tiempo, seguido por los datos de cada partícula.

    Retorna:
        Un diccionario que mapea el tiempo (float) a un DataFrame de pandas
        con los datos de las partículas en ese instante.
    """
    print(f"Leyendo el archivo de simulación: {filename}...")

    # Lectura más rápida usando readlines
    with open(filename, 'r') as f:
        lines = f.readlines()

    timesteps = {}
    current_time = None
    particle_data = []

    for line in lines:
        line = line.strip()
        if not line:
            continue

        parts = line.split(';')

        # Una línea de encabezado de galaxia tiene 5 partes: tiempo, nombre, x, y, z
        if len(parts) == 5 and 'Galaxy' in parts[1]:
            try:
                time_val = float(parts[0])
                # Si ya teníamos datos de partículas, los guardamos
                if current_time is not None and particle_data:
                    # Conversión más rápida a DataFrame usando dtype específico
                    df = pd.DataFrame(particle_data, columns=['id', 'x', 'y', 'z', 'vx', 'vy', 'vz', 'fx', 'fy', 'fz'])
                    df = df.astype(float)
                    timesteps[current_time] = df

                # Empezamos un nuevo paso de tiempo
                current_time = time_val
                particle_data = []
            except (ValueError, IndexError):
                continue
        # Una línea de partícula tiene 10 partes
        elif len(parts) == 10:
            if current_time is not None:
                particle_data.append(parts)

    # Guarda el último bloque de datos
    if current_time is not None and particle_data:
        df = pd.DataFrame(particle_data, columns=['id', 'x', 'y', 'z', 'vx', 'vy', 'vz', 'fx', 'fy', 'fz'])
        df = df.astype(float)
        timesteps[current_time] = df

    print(f"Se procesaron {len(timesteps)} pasos de tiempo.")
    return timesteps

def calculate_kinetic_energy(particles_df):
    """
    Calcula la energía cinética total del sistema.
    KE = 0.5 * m * v^2
    """
    velocities_sq = particles_df[['vx', 'vy', 'vz']]**2
    kinetic_energies = 0.5 * MASS * velocities_sq.sum(axis=1)
    return kinetic_energies.sum()

def calculate_potential_energy(particles_df):
    """
    Calcula la energía potencial gravitacional del sistema de forma optimizada.
    PE = Σ(-G*m_i*m_j / sqrt(r_ij^2 + h^2))

    Usa scipy.spatial.distance para cálculo vectorizado más rápido.
    """
    positions = particles_df[['x', 'y', 'z']].values

    # Cálculo vectorizado de distancias usando scipy
    # pdist calcula todas las distancias pareadas de forma eficiente
    distances = pdist(positions, metric='euclidean')

    # Aplicar la fórmula de energía potencial con suavizado
    # PE = -G * m^2 / sqrt(r^2 + h^2) para cada par
    potential_energies = G * MASS * MASS / np.sqrt(distances**2 + H**2)

    return potential_energies.sum()

def calculate_total_energy(particles_df):
    """
    Calcula la energía total (Cinética + Potencial) del sistema.
    """
    ke = calculate_kinetic_energy(particles_df)
    pe = calculate_potential_energy(particles_df)
    return ke, pe, ke + pe

def main():
    """
    Función principal para ejecutar el análisis.
    """
    # --- CONSTRUCCIÓN DE LA RUTA AL ARCHIVO ---
    try:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        data_file_path = os.path.join(script_dir, "data", "sim.csv")
    except NameError:
        data_file_path = "python/data/sim.csv"

    # 1. Procesar el archivo de datos
    try:
        print(f"Buscando archivo de datos en: {os.path.abspath(data_file_path)}")
        data_by_time = parse_simulation_output(data_file_path)
    except FileNotFoundError:
        print(f"Error: No se encontró el archivo en '{data_file_path}'.")
        print("Asegúrate de que la simulación en Java se haya ejecutado y guardado el archivo en la carpeta 'python/data/'.")
        return

    if not data_by_time:
        print("No se procesaron datos. El archivo 'sim.csv' podría estar vacío o en un formato incorrecto.")
        return

    # 2. Calcular las energías para cada paso de tiempo
    print("\nCalculando energías...")
    times = sorted(data_by_time.keys())
    kinetic_energies = []
    potential_energies = []
    total_energies = []

    for i, t in enumerate(times):
        particles = data_by_time[t]
        ke, pe, total = calculate_total_energy(particles)
        kinetic_energies.append(ke)
        potential_energies.append(pe)
        total_energies.append(total)

        # Mostrar progreso cada 10% aproximadamente
        if i % max(1, len(times) // 10) == 0 or i == len(times) - 1:
            print(f"Progreso: {i+1}/{len(times)} - Tiempo: {t:.4f}s, KE: {ke:.4f}, PE: {pe:.4f}, Total: {total:.4f}")

    # 3. Calcular el error relativo respecto a la energía inicial
    initial_energy = total_energies[0]
    relative_errors = [(E - initial_energy) / abs(initial_energy) * 100 for E in total_energies]

    print(f"\n=== RESUMEN ===")
    print(f"Energía inicial: {initial_energy:.6f}")
    print(f"Energía final: {total_energies[-1]:.6f}")
    print(f"Cambio absoluto: {total_energies[-1] - initial_energy:.6f}")
    print(f"Error relativo final: {relative_errors[-1]:.6f}%")

    # 4. Crear gráfico de energías
    output_dir = os.path.dirname(data_file_path)

    plt.figure(figsize=(14, 6))

    # Gráfico 1: Las tres energías en uno
    #plt.subplot(1, 2, 1)
    plt.plot(times, kinetic_energies, label='Energía Cinética', linewidth=2, alpha=0.8)
    plt.plot(times, potential_energies, label='Energía Potencial', linewidth=2, alpha=0.8)
    plt.plot(times, total_energies, label='Energía Total', linewidth=2, alpha=0.9, color='black', linestyle='--')
    plt.axhline(y=initial_energy, color='red', linestyle=':', alpha=0.5, label=f'E₀ = {initial_energy:.2f}')
    plt.xlabel('Tiempo de Simulación (s)', fontsize=11)
    plt.ylabel('Energía', fontsize=11)
    plt.title('Evolución de las Energías del Sistema', fontsize=12, fontweight='bold')
    plt.legend(loc='best')
    plt.grid(True, alpha=0.3)

    # # Gráfico 2: Error relativo
    # plt.subplot(1, 2, 2)
    # plt.plot(times, relative_errors, color='red', linewidth=2)
    # plt.axhline(y=0, color='black', linestyle='--', alpha=0.5)
    # plt.xlabel('Tiempo de Simulación (s)', fontsize=11)
    # plt.ylabel('Error Relativo (%)', fontsize=11)
    # plt.title('Error Relativo de Conservación de Energía', fontsize=12, fontweight='bold')
    # plt.grid(True, alpha=0.3)

    # Guardar gráfico combinado
    combined_output = os.path.join(output_dir, "energy_analysis.png")
    plt.savefig(combined_output, dpi=150)
    print(f"\n¡Análisis completo! Gráfico guardado en: {combined_output}")

    plt.show()

if __name__ == "__main__":
    main()