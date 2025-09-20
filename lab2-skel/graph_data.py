import pandas as pd
import matplotlib.pyplot as plt
import sys

def main():
    csv_file_path = sys.argv[1]
    df = pd.read_csv(csv_file_path)
    
    plt.figure(figsize=(10, 6))
    
    for sorter, group in df.groupby('sorter_name'):
        group = group.sort_values('thread_count')
        plt.plot(
            group['thread_count'],
            group['average_time'],
            label=sorter,
            marker='o',
            linestyle='-',
            linewidth=2,
            markersize=6
        )
    
    plt.xlabel('Thread Count', fontsize=12)
    plt.ylabel('Average Time (s)', fontsize=12)
    plt.title('Average Time vs Thread Count by Sorter', fontsize=14)
    plt.legend(title='Sorter Name', bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.grid(True, alpha=0.3)
    
    # Adjust layout to prevent legend cutoff
    plt.tight_layout()

    plt.savefig('thread_performance.png', dpi=300, bbox_inches='tight')

if __name__ == "__main__":
    main()