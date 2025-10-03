import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import os
from energy_analysis import calculate_energies, parse_simulation_output

def energy_stddev_for_file(filename):
    if not os.path.isfile(filename):
        raise FileNotFoundError(f"El archivo {filename} no existe.")

    data_by_time = parse_simulation_output(filename)

    times = sorted(data_by_time.keys())
    total_energies = []

    for t in times:
        particles = data_by_time[t]
        _, _, te = calculate_energies(particles)
        total_energies.append(te)

    return np.std(total_energies)


def main():
    """
    Función principal que analiza múltiples archivos de simulación generados
    con diferentes timeSteps y grafica la estabilidad de la energía.
    """
    # 1. Define los timeSteps que has simulado en Java
    timesteps_to_analyze = [0.1, 0.01, 0.001, 0.0001]

    # 2. Define la ruta a la carpeta donde están tus archivos de datos
    try:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        data_directory = os.path.join(script_dir, "data")
    except NameError:
        data_directory = "python/data"

    results = {}

    # 3. Itera sobre cada timeStep, construye el nombre del archivo y analiza
    for dt in timesteps_to_analyze:
        # El formato del nombre del archivo debe coincidir con cómo los guardaste
        filename = os.path.join(data_directory, f"sim_dt_{dt}.csv")

        std_dev = energy_stddev_for_file(filename)

        if std_dev is not None:
            results[dt] = std_dev

    if not results:
        print("No se pudo analizar ningún archivo. Asegúrate de que los archivos CSV existan")
        print("en la carpeta 'data' y que los nombres coincidan (ej: 'sim_dt_0.01.csv').")
        return

    # 4. Prepara los datos para el gráfico
    sorted_timesteps = sorted(results.keys())
    sorted_std_devs = [results[dt] for dt in sorted_timesteps]

    # 5. Crea el gráfico
    plt.style.use('seaborn-v0_8-whitegrid')
    fig, ax = plt.subplots(figsize=(10, 6))

    ax.plot(sorted_timesteps, sorted_std_devs, marker='o', linestyle='-', color='b')

    x_positions = np.arange(len(sorted_timesteps))
    ax.plot(x_positions, sorted_std_devs, marker='o', linestyle='-', color='b')

    # Establecer las etiquetas del eje X para que muestren los valores de dt
    ax.set_xticks(x_positions)
    ax.set_xticklabels([str(dt) for dt in sorted_timesteps])

    ax.set_xlabel('Paso de Tiempo (dt) [s]')
    ax.set_ylabel('Desviación Estándar de la Energía Total')

    ax.grid(True, which="both", ls="--")

    print("\n--- Resumen de Resultados ---")
    for dt, std in results.items():
        print(f"dt = {dt:<8} -> Desv. Estándar = {std:.6f}")

    plt.tight_layout()
    plt.show()




if __name__ == "__main__":
    main()