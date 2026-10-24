package edu.sv.ues.dam235.parcial2.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import edu.sv.ues.dam235.parcial2.R;
import edu.sv.ues.dam235.parcial2.data.model.Reserva;
import java.util.ArrayList;
import java.util.List;

public class ReservaAdapter extends RecyclerView.Adapter<ReservaAdapter.VH> {

    public interface Acciones {
        void editar(Reserva r);
        void eliminar(Reserva r);
    }

    private List<Reserva> datos = new ArrayList<>();
    private final Acciones acciones;

    public ReservaAdapter(Acciones acciones) {
        this.acciones = acciones;
    }

    public void setDatos(List<Reserva> nuevos) {
        datos = nuevos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reserva, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Reserva r = datos.get(position);
        h.tvRestaurante.setText(r.restaurante);
        h.tvDetalle.setText("📅 "  + r.fecha + "   🕒 "  + r.hora
                + "   👥 "  + r.personas + (r.personas == 1 ? " persona" : " personas"));
        h.tvNotas.setText(r.notas);
        h.tvNotas.setVisibility(
                r.notas == null || r.notas.isEmpty() ? View.GONE : View.VISIBLE);
        h.tvEstado.setText(r.pendiente
                ? "⏳ Pendiente de sincronizar"
                : "☁️ Sincronizada con la nube");
        h.btnEditar.setOnClickListener(v -> acciones.editar(r));
        h.btnEliminar.setOnClickListener(v -> acciones.eliminar(r));
    }

    @Override
    public int getItemCount() {
        return datos.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvRestaurante, tvDetalle, tvNotas, tvEstado;
        View btnEditar, btnEliminar;

        VH(@NonNull View itemView) {
            super(itemView);
            tvRestaurante = itemView.findViewById(R.id.tvRestaurante);
            tvDetalle = itemView.findViewById(R.id.tvDetalle);
            tvNotas = itemView.findViewById(R.id.tvNotas);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}