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

def calculate_total_energy(particles_df, return_components=False):
    """
    Calcula la energía total (Cinética + Potencial) para un conjunto
    de partículas en un instante de tiempo.
    
    Parameters:
    -----------
    particles_df : pd.DataFrame
        DataFrame con las columnas ['x', 'y', 'z', 'vx', 'vy', 'vz']
    return_components : bool
        Si es True, retorna (E_total, E_cinética, E_potencial)
    
    Returns:
    --------
    float or tuple : Energía total (puede ser negativa), o tupla con componentes
    
    Notes:
    ------
    - La energía potencial gravitacional es NEGATIVA (fuerzas atractivas)
    - La energía cinética es POSITIVA
    - La energía total E = KE + PE puede ser negativa (sistema ligado)
    - NO tomamos módulo para poder verificar conservación correctamente
    """
    # --- Cálculo de Energía Cinética ---
    velocities_sq = particles_df[['vx', 'vy', 'vz']]**2
    kinetic_energies = 0.5 * MASS * velocities_sq.sum(axis=1)
    total_ke = kinetic_energies.sum()

    # --- Cálculo de Energía Potencial ---
    total_pe = 0.0
    positions = particles_df[['x', 'y', 'z']].values
    
    for i, j in combinations(range(len(positions)), 2):
        pos_i = positions[i]
        pos_j = positions[j]
        
        dist_sq = np.sum((pos_i - pos_j)**2)
        pe_pair = -G * MASS * MASS / np.sqrt(dist_sq + H**2)
        total_pe += pe_pair
    
    # Energía total (SIN módulo)
    total_energy = total_ke + total_pe
    
    if return_components:
        return total_energy, total_ke, total_pe
    
    return total_energy

def calculate_total_energy_vectorized(particles_df, return_components=False):
    """
    Versión vectorizada - SIN módulo en energía total
    """
    # --- Energía Cinética ---
    velocities_sq = particles_df[['vx', 'vy', 'vz']].values**2
    total_ke = 0.5 * MASS * np.sum(velocities_sq)

    # --- Energía Potencial (vectorizado) ---
    positions = particles_df[['x', 'y', 'z']].values
    
    diff = positions[:, np.newaxis, :] - positions[np.newaxis, :, :]
    dist_sq = np.sum(diff**2, axis=2)
    np.fill_diagonal(dist_sq, np.inf)
    
    pe_matrix = -G * MASS * MASS / np.sqrt(dist_sq + H**2)
    total_pe = 0.5 * np.sum(pe_matrix[~np.isinf(pe_matrix)])
    
    # Energía total (SIN módulo)
    total_energy = total_ke + total_pe
    
    if return_components:
        return total_energy, total_ke, total_pe
    
    return total_energy

def check_energy_conservation(times, energies, tolerance=0.01):
    """
    Verifica si la energía se conserva dentro de una tolerancia.
    Ahora usa valor absoluto del error sobre valor absoluto de energía inicial.
    """
    energy_array = np.array(energies)
    initial_energy = energy_array[0]
    
    # Error absoluto y relativo (usando |E_inicial| para normalizar)
    abs_error = np.abs(energy_array - initial_energy)
    rel_error = abs_error / np.abs(initial_energy)
    
    max_abs_error = np.max(abs_error)
    max_rel_error = np.max(rel_error)
    mean_rel_error = np.mean(rel_error)
    
    stats = {
        'initial_energy': initial_energy,
        'final_energy': energy_array[-1],
        'max_absolute_error': max_abs_error,
        'max_relative_error': max_rel_error,
        'mean_relative_error': mean_rel_error,
        'is_conserved': max_rel_error < tolerance,
        'tolerance': tolerance
    }
    
    return stats


def plot_energy_analysis(times, total_energies, kinetic_energies, potential_energies, 
                         output_dir, stats):
    """
    Crea gráficos detallados del análisis de energía.
    """
    # Crear figura con múltiples subplots
    fig, axes = plt.subplots(2, 2, figsize=(16, 12))
    fig.suptitle('Análisis Completo de Energía - Simulación de Galaxias', fontsize=16, fontweight='bold')
    
    # --- Subplot 1: Todas las Energías (Escala Normal) ---
    ax1 = axes[0, 0]
    # Energía Total (puede ser negativa - sistema ligado)
    ax1.plot(times, total_energies, 'b-', linewidth=2, label='E Total', alpha=0.8)
    # Energía Cinética (siempre positiva)
    ax1.plot(times, kinetic_energies, 'g-', linewidth=2, label='E Cinética', alpha=0.8)
    # Energía Potencial (negativa - mostramos como está)
    ax1.plot(times, potential_energies, 'orange', linewidth=2, 
             label='E Potencial', alpha=0.8)
    
    initial_energy = total_energies[0]
    ax1.axhline(y=initial_energy, color='r', linestyle='--', linewidth=1.5, 
                label=f'E Inicial ({initial_energy:.4e})', alpha=0.6)
    ax1.axhline(y=0, color='black', linestyle='-', linewidth=0.5, alpha=0.3)
    
    ax1.set_xlabel('Tiempo de Simulación', fontsize=12)
    ax1.set_ylabel('Energía', fontsize=12)
    ax1.set_title('Evolución de las Componentes de Energía', fontsize=13, fontweight='bold')
    ax1.grid(True, alpha=0.3)
    ax1.legend(fontsize=10, loc='best')
    
    # --- Subplot 2: Energías en Valor Absoluto (Escala Log) ---
    ax2 = axes[0, 1]
    ax2.plot(times, np.abs(total_energies), 'b-', linewidth=2, label='|E Total|', alpha=0.8)
    ax2.plot(times, kinetic_energies, 'g-', linewidth=2, label='E Cinética', alpha=0.8)
    ax2.plot(times, np.abs(potential_energies), 'orange', linewidth=2, 
             label='|E Potencial|', alpha=0.8)
    
    ax2.set_xlabel('Tiempo de Simulación', fontsize=12)
    ax2.set_ylabel('|Energía| (escala log)', fontsize=12)
    ax2.set_title('Magnitud de Componentes (Escala Logarítmica)', fontsize=13, fontweight='bold')
    ax2.grid(True, alpha=0.3, which='both')
    ax2.legend(fontsize=10, loc='best')
    ax2.set_yscale('log')
    
    # --- Subplot 3: Error Relativo ---
    ax3 = axes[1, 0]
    rel_error = np.abs(np.array(total_energies) - initial_energy) / np.abs(initial_energy) * 100
    ax3.plot(times, rel_error, 'r-', linewidth=2)
    ax3.axhline(y=stats['tolerance']*100, color='orange', linestyle='--', 
                linewidth=2, label=f"Tolerancia ({stats['tolerance']*100:.1f}%)")
    ax3.set_xlabel('Tiempo de Simulación', fontsize=12)
    ax3.set_ylabel('Error Relativo (%)', fontsize=12)
    ax3.set_title('Error Relativo en la Conservación de Energía', fontsize=13, fontweight='bold')
    ax3.grid(True, alpha=0.3)
    ax3.legend(fontsize=10)
    
    # --- Subplot 4: Estadísticas ---
    ax4 = axes[1, 1]
    ax4.axis('off')
    
    stats_text = f"""
    ESTADÍSTICAS DE CONSERVACIÓN DE ENERGÍA
    {'='*50}
    
    Energía Inicial:           {stats['initial_energy']:.6e}
    Energía Final:             {stats['final_energy']:.6e}
    
    Error Absoluto Máximo:     {stats['max_absolute_error']:.6e}
    Error Relativo Máximo:     {stats['max_relative_error']*100:.4f}%
    Error Relativo Promedio:   {stats['mean_relative_error']*100:.4f}%
    
    Tolerancia:                {stats['tolerance']*100:.2f}%
    
    ¿Se conserva la energía?   {'✓ SÍ' if stats['is_conserved'] else '✗ NO'}
    
    {'='*50}
    
    Nota: Sistema gravitacional ligado (E_total < 0)
    E_total = E_cinética + E_potencial debe ser constante
    """
    
    ax4.text(0.1, 0.5, stats_text, fontsize=11, family='monospace',
             verticalalignment='center',
             bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.3))
    
    plt.tight_layout()
    
    output_filename = os.path.join(output_dir, "energy_analysis_complete.png")
    plt.savefig(output_filename, dpi=150, bbox_inches='tight')
    print(f"\n✓ Gráfico completo guardado en: {output_filename}")
    
    # --- Gráfico Simple ---
    plt.figure(figsize=(14, 8))
    
    plt.plot(times, total_energies, 'b-', linewidth=2.5, label='Energía Total', alpha=0.9)
    plt.plot(times, kinetic_energies, 'g-', linewidth=2, label='Energía Cinética', alpha=0.8)
    plt.plot(times, potential_energies, 'orange', linewidth=2, 
             label='Energía Potencial', alpha=0.8)
    
    plt.axhline(y=initial_energy, color='r', linestyle='--', linewidth=2, 
                label=f'Energía Inicial ({initial_energy:.4e})', alpha=0.6)
    plt.axhline(y=0, color='black', linestyle='-', linewidth=0.5, alpha=0.3)
    
    plt.xlabel('Tiempo de Simulación', fontsize=14)
    plt.ylabel('Energía', fontsize=14)
    plt.title('Conservación y Componentes de Energía del Sistema', fontsize=16, fontweight='bold')
    plt.grid(True, alpha=0.3)
    plt.legend(fontsize=12, loc='best')
    plt.tight_layout()
    
    simple_output = os.path.join(output_dir, "total_energy_plot.png")
    plt.savefig(simple_output, dpi=150, bbox_inches='tight')
    print(f"✓ Gráfico simple guardado en: {simple_output}")
    
    plt.close('all')


def main():
    """
    Función principal para ejecutar el análisis.
    """
    # --- CONSTRUCCIÓN DE LA RUTA AL ARCHIVO ---
    try:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        data_file_path = os.path.join(script_dir, "data", "sim.csv")
        output_dir = os.path.dirname(data_file_path)
    except NameError:
        data_file_path = "python/data/sim.csv"
        output_dir = "python/data"

    # 1. Procesar el archivo de datos
    try:
        print(f"Buscando archivo de datos en: {os.path.abspath(data_file_path)}")
        data_by_time = parse_simulation_output(data_file_path)
    except FileNotFoundError:
        print(f"❌ Error: No se encontró el archivo en '{data_file_path}'.")
        print("Asegúrate de que la simulación en Java se haya ejecutado correctamente.")
        return

    if not data_by_time:
        print("❌ No se procesaron datos. El archivo 'sim.csv' podría estar vacío o en formato incorrecto.")
        return

    # 2. Determinar qué función usar según el número de partículas
    times = sorted(data_by_time.keys())
    first_frame = data_by_time[times[0]]
    n_particles = len(first_frame)
    
    print(f"\n{'='*60}")
    print(f"Número de partículas: {n_particles}")
    
    # Usar versión vectorizada para N > 100
    if n_particles > 100:
        print("Usando versión VECTORIZADA (optimizada para muchas partículas)")
        energy_func = calculate_total_energy_vectorized
    else:
        print("Usando versión ESTÁNDAR")
        energy_func = calculate_total_energy
    
    print(f"{'='*60}\n")

    # 3. Calcular energías para cada paso de tiempo
    print("Calculando energías en cada paso temporal...")
    total_energies = []
    kinetic_energies = []
    potential_energies = []

    for i, t in enumerate(times):
        particles = data_by_time[t]
        
        # Calcular energía con componentes (SIN módulo)
        total_energy, ke, pe = energy_func(particles, return_components=True)
        
        total_energies.append(total_energy)  # Ya NO es valor absoluto
        kinetic_energies.append(ke)
        potential_energies.append(pe)
        
        # Mostrar progreso
        if i % max(1, len(times)//10) == 0:
            progress = (i / len(times)) * 100
            print(f"  Progreso: {progress:.0f}% - Tiempo: {t:.2f}, E_total: {total_energy:.4e}, "
                f"KE: {ke:.4e}, PE: {pe:.4e}")
    print(f"✓ Cálculo completado para {len(times)} pasos temporales\n")

    # 4. Verificar conservación de energía
    print("Analizando conservación de energía...")
    stats = check_energy_conservation(times, total_energies, tolerance=0.01)
    
    print(f"\n{'='*60}")
    print("RESULTADOS DEL ANÁLISIS")
    print(f"{'='*60}")
    print(f"Energía inicial:          {stats['initial_energy']:.6e}")
    print(f"Energía final:            {stats['final_energy']:.6e}")
    print(f"Error relativo máximo:    {stats['max_relative_error']*100:.4f}%")
    print(f"Error relativo promedio:  {stats['mean_relative_error']*100:.4f}%")
    print(f"Tolerancia:               {stats['tolerance']*100:.2f}%")
    print(f"\n¿Se conserva la energía?  {'✓ SÍ' if stats['is_conserved'] else '✗ NO'}")
    print(f"{'='*60}\n")

    # 5. Crear gráficos
    print("Generando gráficos...")
    plot_energy_analysis(times, total_energies, kinetic_energies, potential_energies, 
                         output_dir, stats)
    
    print(f"\n{'='*60}")
    print("¡ANÁLISIS COMPLETO!")
    print(f"{'='*60}\n")

if __name__ == "__main__":
    main()