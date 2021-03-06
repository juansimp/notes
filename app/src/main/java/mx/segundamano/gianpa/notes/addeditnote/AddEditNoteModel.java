package mx.segundamano.gianpa.notes.addeditnote;

import android.net.Uri;

import io.realm.Realm;
import io.realm.RealmModel;
import mx.segundamano.gianpa.notes.Note;
import mx.segundamano.gianpa.notes.NoteRealmModel;
import mx.segundamano.gianpa.notes.NotesService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AddEditNoteModel {

    private final NotesService service;
    private Realm realm;

    public AddEditNoteModel() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.myjson.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(NotesService.class);

        realm = Realm.getDefaultInstance();
    }


    public void save(Note note, AddEditNoteModelCallback addEditNoteModelCallback) {
        if (note.id == null) {
            create(note.title, note.body, addEditNoteModelCallback);
        } else {
            update(note.id, note.title, note.body, addEditNoteModelCallback);
        }
    }

    private void create(final String title, final String body, final AddEditNoteModelCallback callback) {
        NoteApiModel noteApiModel = new NoteApiModel(title, body);

        Call<MyJsonResponse> call = service.create(noteApiModel);
        call.enqueue(new Callback<MyJsonResponse>() {
            @Override
            public void onResponse(final Call<MyJsonResponse> call, Response<MyJsonResponse> response) {
                if (!response.isSuccessful()) {
                    callback.onError(new Exception(response.message()));
                    return;
                }

                Uri uri = Uri.parse(response.body().uri);

                persist(uri.getLastPathSegment(), title, body, callback);
            }

            @Override
            public void onFailure(Call<MyJsonResponse> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    private void update(final String id, final String title, final String body, final AddEditNoteModelCallback callback) {
        final NoteApiModel noteApiModel = new NoteApiModel(title, body);

        Call<NoteApiModel> call = service.update(id, noteApiModel);
        call.enqueue(new Callback<NoteApiModel>() {
            @Override
            public void onResponse(final Call<NoteApiModel> call, Response<NoteApiModel> response) {
                if (!response.isSuccessful()) {
                    callback.onError(new Exception(response.message()));
                    return;
                }

                persist(id, title, body, callback);
            }

            @Override
            public void onFailure(Call<NoteApiModel> call, Throwable t) {
                callback.onError(t);
            }
        });
    }

    private void persist(final String id, final String title, final String body, final AddEditNoteModelCallback callback) {
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmModel noteRealmModel = new NoteRealmModel(id, title, body);
                realm.copyToRealmOrUpdate(noteRealmModel);
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                callback.onSuccess(new Note(id, title, body));
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                callback.onError(error);
            }
        });
    }
}
