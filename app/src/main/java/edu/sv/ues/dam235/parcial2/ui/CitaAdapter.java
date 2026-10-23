package edu.sv.ues.dam235.parcial2.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import edu.sv.ues.dam235.parcial2.R;
import edu.sv.ues.dam235.parcial2.data.model.Cita;
import java.util.ArrayList;
import java.util.List;

public class CitaAdapter extends RecyclerView.Adapter<CitaAdapter.VH> {

    public interface Acciones {
        void editar(Cita c);
        void eliminar(Cita c);
    }

    private List<Cita> datos = new ArrayList<>();
    private final Acciones acciones;

    public CitaAdapter(Acciones acciones) {
        this.acciones = acciones;
    }

    public void setDatos(List<Cita> nuevos) {
        datos = nuevos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cita, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Cita c = datos.get(position);
        h.tvTitulo.setText(c.titulo);
        h.tvFechaHora.setText("📅 " + c.fecha + "   🕒 " + c.hora);
        h.tvDescripcion.setText(c.descripcion);
        h.tvDescripcion.setVisibility(
                c.descripcion == null || c.descripcion.isEmpty() ? View.GONE : View.VISIBLE);
        h.tvEstado.setText(c.pendiente
                ? "⏳ Pendiente de sincronizar"
                : "☁️ Sincronizada con la nube");
        h.btnEditar.setOnClickListener(v -> acciones.editar(c));
        h.btnEliminar.setOnClickListener(v -> acciones.eliminar(c));
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvFechaHora, tvDescripcion, tvEstado;
        View btnEditar, btnEliminar;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvFechaHora = itemView.findViewById(R.id.tvFechaHora);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}